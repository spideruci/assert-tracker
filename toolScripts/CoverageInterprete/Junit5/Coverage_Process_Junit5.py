#!/usr/bin/env python
# coding: utf-8

# In[2]:


import re
import operator
import pickle
import copy
import hashlib
import time
import copy
import xml.etree.ElementTree as ET
import networkx as nx
from bs4 import BeautifulSoup
import lxml
from xml.dom import minidom
import zipfile
import numpy as np
import bisect
from lxml import etree


# In[3]:


base1 = "3-coverage-scan1"
base2 = "3-coverage-scan2"
base3 = "3-coverage-scan3"
base4 = "3-coverage-scan4"
zipfilepath = 'target/xmlOutput.zip'
largest_file_size=100000000


# In[4]:


def Lines(base):
    with open(base+"/target/test_info.txt", encoding='utf-8',errors = "ignore") as inf:
        for line in inf:
            yield line


# In[16]:


def init_process(base):
    zipfilepath = 'target/xmlOutput.zip'
    early_time=None
    late_time=None
#     with open(base+"/target/test_info.txt", encoding='utf-8',errors = "ignore") as inf:
#         Lines = inf.readlines()
    
    normal_state={}
    current_test_class = None
    current_test_method = None

    current_test_case={}
    current_states={}
    count=0
    class_name=None
    method_name=None
    prev=[False,False,False,False,False,False]
    test_case_queue=[]
    flag_exit=False
    for line in Lines(base):

        
        
        
        
        enterBeforeAll = len(re.findall("enter BeforeAll method",line))!=0
        enterBeforeEach = len(re.findall("enter BeforeEach method",line))!=0
        exitAfterAll = len(re.findall("exit AfterAll method",line))!=0
        exitAfterEach = len(re.findall("exit AfterEach method",line))!=0
        constructor = len(re.findall("Enter Junit5 Constructor",line))!=0 and len(re.findall("NotSpecified",line))==0
        # enter_method requires setUp and tearDown at first
        enter_method = len(re.findall("Start executing",line))!=0
        exit_method = len(re.findall("Finish executing outer test method",line))!=0

        isAssertion = len(re.findall("Compiled at",line))!=0
        
        cur_time = line.split(" ")[-1][:-1]
        if enter_method:
            if(early_time==None):
                early_time =cur_time
            _,_,_,_,_,method_name,_,class_name,_ = line.split(" ")
            current_test_case["class"]=class_name
            current_test_case["method"]=method_name
        elif enterBeforeEach:
            early_time = cur_time
        elif exitAfterEach:
            late_time=cur_time
        elif exit_method:
            late_time=cur_time
            #handle assume
            flag_exit=True
        elif constructor:

            if (len(current_test_case)!=0 and flag_exit):
                if class_name not in normal_state:
                    normal_state[class_name]={}
                if method_name not in normal_state[class_name]:
                    normal_state[class_name][method_name]=[]
                current_test_case["start"]=early_time
                current_test_case["end"]=late_time
                normal_state[class_name][method_name].append(current_test_case)
                count=count+1
            flag_exit=False
            current_test_case={}
            early_time=None
            late_time=None



        

    if(flag_exit and len(current_test_case)!=0):
        if class_name not in normal_state:
            normal_state[class_name]={}
        if method_name not in normal_state[class_name]:
            normal_state[class_name][method_name]=[]
        current_test_case["start"]=early_time
        current_test_case["end"]=late_time
        normal_state[class_name][method_name].append(current_test_case)
        count=count+1
    print("received "+ str(count)+" test cases, please verify the number reported here is consistent with other reports")
    return normal_state


# In[17]:


def match_file(base):
    normal_state = init_process(base)
    zips = zipfile.ZipFile(base+"/"+zipfilepath)
    sorted_filelist = sorted(zips.filelist, key=lambda file: int(file.filename.split(" ")[-1][:-4]))
    zips_time=[int(f.filename.split(" ")[-1][:-4]) for f in sorted_filelist]
    for class_name,class_content in normal_state.items():
        for method_name, method_content in class_content.items():
            for index,test_case in enumerate(method_content):

                test_case["local_variables"]=[]

                #match time duration2
                lower_bound = int(test_case["start"])
                upper_bound = int(test_case["end"])
                lower_bound_i = bisect.bisect_left(zips_time, lower_bound)
                upper_bound_i = bisect.bisect_right(zips_time, upper_bound, lo=lower_bound_i)

                for i in range(lower_bound_i,upper_bound_i):
                    test_case["local_variables"].append(sorted_filelist[i].filename)
    return normal_state
    
            


# In[18]:


print("first coverage scan: "+ "="*100)
zips1 = zipfile.ZipFile(base1+"/"+zipfilepath)
normal_state1 = match_file(base1)


# In[19]:


print("Second coverage scan: "+ "="*100)
zips2 = zipfile.ZipFile(base2+"/"+zipfilepath)
normal_state2 = match_file(base2)


# In[20]:


print("Third coverage scan: "+ "="*100)
zips3 = zipfile.ZipFile(base3+"/"+zipfilepath)
normal_state3 = match_file(base3)


# In[21]:


print("Fourth coverage scan: "+ "="*100)
zips4 = zipfile.ZipFile(base4+"/"+zipfilepath)
normal_state4 = match_file(base4)


# In[22]:


def get_graph_from_root(root):
    object_dict = {}
    root_id = root.attrib.get('id')
    if root_id!=None:
        object_dict[root_id]=root
    queue = [root]
    G = nx.DiGraph()
    G.add_edge(root,root)
    while len(queue)!=0:
        current_node = queue.pop(0)
        for child in current_node:
            queue.append(child)
            child_id = child.attrib.get('id')
            child_reference = child.attrib.get('reference')
            if child_id!=None:
                object_dict[child_id]=child 
            if child_reference!=None:
                object_dict[child_reference]=child 

            if child_reference!=None :
                G.add_edge(current_node,object_dict[child_reference])
            else:
                G.add_edge(current_node,child)
    
    return G


# In[23]:


def get_state_files(test_case):
    for file_name in test_case["local_variables"]:
        yield file_name


# In[25]:


comparison_info = {}

zips=[zips1,zips2,zips3,zips4]
for test_class,class_content in normal_state3.items():

    for test_method, method_content in class_content.items():
        num = len(normal_state1)
        
        test_cases1 = normal_state1[test_class][test_method]
        test_cases2 = normal_state2[test_class][test_method]
        test_cases3 = normal_state3[test_class][test_method]
        test_cases4 = normal_state4[test_class][test_method]
        num = len(test_cases1)
        for i in range(num):
            comparison_info[test_class+" "+test_method+" "+str(i)]={}
            test_case1,test_case2,test_case3,test_case4 = test_cases1[i],test_cases2[i],test_cases3[i],test_cases4[i]

            state_files1 = [file for file in get_state_files(test_case1)]
            state_files2 = [file for file in get_state_files(test_case2)]
            state_files3 = [file for file in get_state_files(test_case3)]
            state_files4 = [file for file in get_state_files(test_case4)]
            # assert length equal
            if not (len(state_files1)==len(state_files2)==len(state_files3)==len(state_files4)):
                comparison_info[test_class+" "+test_method+" "+str(i)]["forbidden"]=True
                print("-"*30)
                print(test_class+" "+test_method+" "+"have flaky number of executed assertion statements")
                continue
            # avoid comparing large files

            for state_index in range(len(state_files1)):
                files = [state_files1[state_index],state_files2[state_index],state_files3[state_index],state_files4[state_index]]
                for index,file in enumerate(files):
                    if zips[index].getinfo(files[index]).file_size>=largest_file_size:
                        comparison_info[test_class+" "+test_method+" "+str(i)][state_index]= None
                        print("-"*30)
                        print("variables at "+str(state_index)+" in "+test_class+" "+test_method+" are too large to parse")
                        #num of executed assertion statements are flaky

            for state_index in range(len(state_files1)):
                if state_index not in comparison_info[test_class+" "+test_method+" "+str(i)]:
                    files = [state_files1[state_index],state_files2[state_index],state_files3[state_index],state_files4[state_index]]
                    data = [zips[_].read(files[_])for _ in range(4)]
                    utf_8_data = [_.decode("utf-8",errors="ignore") for _ in data if not isinstance(_,str)]
                    roots =  [ET.fromstring(_,parser=etree.XMLParser(recover=True)) for _ in utf_8_data]
                    Graphs = [get_graph_from_root(root) for root in roots]
                    all_elements = [list(nx.bfs_tree(Graphs[_], source=roots[_])) for _ in range(4)]
                    if (not len(all_elements[0])==len(all_elements[1])==len(all_elements[2])==len(all_elements[3])):
                        comparison_info[test_class+" "+test_method+" "+str(i)][state_index]=None
                        print("number of elements in assertion "+str(state_index)+" "+ test_class+" "+test_method+" are flaky") 
                        #num of elements are flaky
                    else:
                        elements1 = all_elements[0]
                        elements2 = all_elements[1]
                        elements3 = all_elements[2]
                        elements4 = all_elements[3]
                        for e_index in range(len(elements1)):
                            element1 = elements1[e_index]
                            element2 = elements2[e_index]
                            element3 = elements3[e_index]
                            element4 = elements4[e_index]
                            if (not element1.tag==element2.tag==element3.tag==element4.tag) or (not element1.text==element2.text==element3.text==element4.text):
                                print("-"*15+"flakiness detail"+"-"*15)
                                print(test_class+" "+test_method+" before assertion "+str(state_index))
                                print(element1.tag)
#                                 print(element1.tag+ " "+element1.text)
#                                 print(element2.tag+ " "+element2.text)
#                                 print(element3.tag+ " "+element3.text)
#                                 print(element4.tag+ " "+element4.text)
                                if state_index not in comparison_info[test_class+" "+test_method+" "+str(i)]:
                                    comparison_info[test_class+" "+test_method+" "+str(i)][state_index]=set()
                                comparison_info[test_class+" "+test_method+" "+str(i)][state_index].add(element1.tag)
                                comparison_info[test_class+" "+test_method+" "+str(i)][state_index].add(element2.tag)
                                comparison_info[test_class+" "+test_method+" "+str(i)][state_index].add(element3.tag)
                                comparison_info[test_class+" "+test_method+" "+str(i)][state_index].add(element4.tag)


# In[27]:


with open("comparison_info",'wb') as f:
    pickle.dump(comparison_info,f)
    
with open("normal_state","wb") as f:
    pickle.dump(normal_state1,f)


# In[ ]:





# In[ ]:





# In[ ]:





# In[ ]:





# In[ ]:





# In[ ]:





# In[ ]:





# In[ ]:




