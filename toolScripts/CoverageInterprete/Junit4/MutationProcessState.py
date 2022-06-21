#!/usr/bin/env python
# coding: utf-8

# In[1]:


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


# In[2]:


zipfilepath = 'target/xmlOutput.zip'


# In[3]:


base = "4-mutation-scan"
def Lines():
    with open(base+"/target/test_result.txt", encoding='utf-8',errors = "ignore") as inf:
        for line in inf:
            yield line


# In[4]:


def read_mutation_detail(detail_str):
    mutation_class = re.findall("clazz=.*?,",detail_str)[0][6:-1]
    mutation_method = re.findall("method=.*?,",detail_str)[0][7:-1]
    mutator = re.findall("mutator=.*?]",detail_str)[0][8:-1]
    result = {}
    file_name = re.findall("filename=[a-zA-Z0-9_.\$]+", detail_str)[0][9:]
    line_number = re.findall("lineNumber=[0-9]+", detail_str)[0][11:]
    test_cases = re.findall("testsInOrder=.*]",detail_str)[0][14:-2]
    #since some test methods include arguments
    test_cases = test_cases.split("), ")
    # some test methods in different classes have the same name
    for test_case in test_cases:
        if (len(re.findall('.*\(',test_case))==0):
            print(test_case)
            raise AssertionError("there are something wrong in test methods")
    test_cases = [re.findall('.*\(',test_case)[0][:-1] for test_case in test_cases]
    test_cases = [ test_case if '[' not in test_case else test_case[0:test_case.index('[')] for test_case in test_cases]
    #check if test_methods have same method names
#     if (len(test_cases)!=len(set(test_cases))):
#         raise AssertionError("there are test methods that share the same name.\n"+"they are: ")
    result["mutation_class"] = mutation_class
    result["mutation_method"]= mutation_method
    result["mutator"]=mutator

    result["file_name"]= file_name
    result["line_number"] = line_number
    return result, test_cases


# In[5]:


with open("normal_state","rb") as f:
    normal_state = pickle.load(f)


# In[6]:


print(normal_state['com.google.gson.CommentsTest'])


# In[7]:



current_test_class = None
current_test_method = None

current_test_case={}
current_states={}
count=0
prev=[False]*10
new_mutation={}
mutated_info = []
test_cases=None
test_case_index=0
flag=False
# prev_constructor=[False,False]
for line in Lines():
    detected = len(re.findall("detected = ",line))!=0
    if(detected):
        flag=True
    mutation_details = re.findall("Running mutation MutationDetails \[.*\]",line)
    if len(mutation_details)!=0:

        if(sum(prev)!=0 and new_mutation["test_cases"][test_case_index]["state"]=="unknown"):
            if prev[0] or prev[2] or prev[4] or prev[6] or prev[8]:
                # the test case failed
                num_start_assertion = new_mutation["test_cases"][test_case_index]["start_assertion"]
                num_end_assertion = new_mutation["test_cases"][test_case_index]["end_assertion"]
                if(num_start_assertion!=num_end_assertion):
                    new_mutation["test_cases"][test_case_index]["state"]="assertion_failure"
                else:
                    new_mutation["test_cases"][test_case_index]["state"]="crash"
                new_mutation["status"]="killed"
            elif sum(prev)!=0:
                new_mutation["test_cases"][test_case_index]["state"]="success"
            test_case_index=test_case_index+1
        
        while(sum(prev)!=0 and test_case_index<len(new_mutation["test_cases"])):
            new_mutation['test_cases'][test_case_index]["state"]="crash"
            test_case_index=test_case_index+1
            
        if(sum(prev)!=0 and flag==False):
            new_mutation["status"]="abnormal"    
        new_mutation = {}
        mutation_info,test_cases = read_mutation_detail(mutation_details[0])
        new_mutation["mutation_info"] = mutation_info
        new_mutation["status"] = "survive"
        new_mutation["test_cases"]=[]
        for t in test_cases:
            parts= t.split(".")
            test_class = ".".join(parts[:-1])
            test_method = parts[-1]
            new_mutation["test_cases"].append({"test_class":test_class,"test_method":test_method,
                                               "start_assertion":0,"end_assertion":0,
                                               "state":"unknown","start":None, "end":None,"local_variables":[]})
        mutated_info.append(new_mutation)
        

        prev=[False]*10
        test_case_index=0
        flag=False
        continue

#     enter_constructor = len(re.findall("Enter Junit4 Constructor",line))!=0    
#     exit_constructor = len(re.findall("Enter Junit4 Constructor",line))!=0  
#     cur_constructor = [enter_constructor, exit_constructor]
#     if(enter_contructor or exit_constructor):
#         prev_constructor = [enter_constructor, exit_constructor]
        
    cur_time = line.split(" ")[-1][:-1]   
    before = len(re.findall("enter Before method",line))!=0
    exit_before = len(re.findall("exit Before",line))!=0
    after = len(re.findall("enter After method",line))!=0
    setUp= len(re.findall("Start executing outer test method: setUp",line))!=0
    tearDown = len(re.findall("Start executing outer test method: tearDown",line))!=0
    # enter_method requires setUp and tearDown at first
    enter_method = len(re.findall("Start executing",line))!=0 and not setUp and not tearDown
    exit_tearDown = len(re.findall("Finish executing outer test method: tearDown",line))!=0
    exit_setUp = len(re.findall("Finish executing outer test method: setUp",line))!=0
    exit_after = len(re.findall("exit After",line))!=0
    exit_method = len(re.findall("Finish executing outer test method",line))!=0 and not exit_tearDown and not exit_setUp

    isAssertion = len(re.findall("Compiled at",line))!=0
    endAssertion = len(re.findall("end:",line))!=0

    cur = (before,exit_before, setUp, exit_setUp, enter_method, exit_method, after,exit_after, tearDown, exit_tearDown )
    has_log = sum(cur)!=0
    
    if isAssertion:
        new_mutation["test_cases"][test_case_index]["start_assertion"]=new_mutation["test_cases"][test_case_index]["start_assertion"]+1
    elif endAssertion:
        new_mutation["test_cases"][test_case_index]["end_assertion"]=new_mutation["test_cases"][test_case_index]["end_assertion"]+1
    elif(prev[0] or prev[2] or prev[4] or prev[6] or prev[8]) and (cur[0] or cur[2] or cur[4]):
        #            ((cur[6] or cur[8]) and (prev[0] or prev[2] or prev[4]))
        # the test case is not correctly finished? Let's see if it's assertion failure or crash!
        if(new_mutation["test_cases"][test_case_index]["state"]=="unknown"):
        
            num_start_assertion = new_mutation["test_cases"][test_case_index]["start_assertion"]
            num_end_assertion = new_mutation["test_cases"][test_case_index]["end_assertion"]
            if(num_start_assertion!=num_end_assertion):
                new_mutation["test_cases"][test_case_index]["state"]="assertion_failure"
            else:
                new_mutation["test_cases"][test_case_index]["state"]="crash"
            new_mutation["status"]="killed"
        test_case_index= test_case_index+1
        if(cur[2] or cur[4]):
            next_test_class = line.split(' ')[10]
            while(new_mutation["test_cases"][test_case_index]["test_class"]!=next_test_class):
                new_mutation["test_cases"][test_case_index]["state"]="crash"
                test_case_index=test_case_index+1
    elif(prev[5] or prev[7] or prev[9]) and (cur[0] or cur[2] or cur[4]):
        if(new_mutation["test_cases"][test_case_index]["state"]=="unknown"):
        # the test case is correctly finished!
#         if(new_mutation["test_cases"][test_case_index]["state"]!="unknown"):
            new_mutation["test_cases"][test_case_index]["state"]="success"
        test_case_index=test_case_index+1
        if(cur[2] or cur[4]):
            next_test_class = line.split(' ')[10]
            while(new_mutation["test_cases"][test_case_index]["test_class"]!=next_test_class):
                new_mutation["test_cases"][test_case_index]["state"]="crash"
                test_case_index=test_case_index+1
    elif((cur[6] or cur[8]) and (prev[0] or prev[2] or prev[4])):
        # "failure", but have to finish "tearDown" or "after"
        num_start_assertion = new_mutation["test_cases"][test_case_index]["start_assertion"]
        num_end_assertion = new_mutation["test_cases"][test_case_index]["end_assertion"]
        if(num_start_assertion!=num_end_assertion):
            new_mutation["test_cases"][test_case_index]["state"]="assertion_failure"
        else:
            new_mutation["test_cases"][test_case_index]["state"]="crash"
        new_mutation["status"]="killed"
#         test_case_index = test_case_index+1

    if has_log:
        prev=cur
    
    if (cur[0] or cur[2] or cur[4]) and new_mutation["test_cases"][test_case_index]["start"]==None:
        new_mutation["test_cases"][test_case_index]["start"]=cur_time
        
    
if new_mutation["test_cases"][test_case_index]["state"]=="unknown":        
    if prev[0] or prev[2] or prev[4] or prev[6] or prev[8]:
        # the test case failed
        num_start_assertion = new_mutation["test_cases"][test_case_index]["start_assertion"]
        num_end_assertion = new_mutation["test_cases"][test_case_index]["end_assertion"]
        if(num_start_assertion!=num_end_assertion):
            new_mutation["test_cases"][test_case_index]["state"]="assertion_failure"
        else:
            new_mutation["test_cases"][test_case_index]["state"]="crash"
        new_mutation["status"]="killed"
    else:
        new_mutation["test_cases"][test_case_index]["state"]="success"
while(test_case_index<len(new_mutation["test_cases"])):
    new_mutation['test_cases'][test_case_index]["state"]="crash"
    test_case_index=test_case_index+1


# In[8]:


pre_test_case=None
cur_test_case=None
for mutation in mutated_info:
    for test_case in mutation["test_cases"]:
        if test_case["start"]!=None:
            pre_test_case = cur_test_case
            cur_test_case = test_case
            if pre_test_case!=None:
                pre_test_case["end"]=cur_test_case["start"]
        
if cur_test_case["start"]!=None:
    cur_test_case["end"]=str(int(cur_test_case["start"])+10000000000)
            


# In[ ]:





# In[9]:


def read_killed_mutation_detail(detail_str):
    mutation_class = re.findall("clazz=.*?,",detail_str)[0][6:-1]
    mutation_method = re.findall("method=.*?,",detail_str)[0][7:-1]
    mutator = re.findall("mutator=.*?]",detail_str)[0][8:-1]
    result = {}

    test_cases = re.findall("detected = KILLED by .*]",detail_str)[0][22:-2]
#     test_cases = re.findall("testsInOrder=.*]",detail_str)[0][14:-2]
    #since some test methods include arguments
    test_cases = test_cases.split("), ")
    # some test methods in different classes have the same name
    for test_case in test_cases:
        if (len(re.findall('.*\(',test_case))==0):
            print(test_case)
            raise AssertionError("there are something wrong in test methods")
    test_cases = [re.findall('.*\(',test_case)[0][:-1] for test_case in test_cases]
    test_cases = [ test_case if '[' not in test_case else test_case[0:test_case.index('[')] for test_case in test_cases]
    #check if test_methods have same method names
#     if (len(test_cases)!=len(set(test_cases))):
#         raise AssertionError("there are test methods that share the same name.\n"+"they are: ")
    result["mutation_class"] = mutation_class
    result["mutation_method"]= mutation_method
    result["mutator"]=mutator

    return result, test_cases


        


# In[10]:



zips = zipfile.ZipFile(base+"/"+zipfilepath)
sorted_filelist = sorted(zips.filelist, key=lambda file: int(file.filename.split(" ")[-1][:-4]))
zips_time=[int(f.filename.split(" ")[-1][:-4]) for f in sorted_filelist]
count=0
for mutation in mutated_info:
    for test_case in mutation["test_cases"]:

        #match time duration2
        if test_case["start"]!=None:
            lower_bound = int(test_case["start"])
            upper_bound = int(test_case["end"])
            lower_bound_i = bisect.bisect_left(zips_time, lower_bound)
            upper_bound_i = bisect.bisect_right(zips_time, upper_bound, lo=lower_bound_i)

#         if(upper_bound_i-lower_bound_i>100):
#             print(test_case)
            for i in range(lower_bound_i,upper_bound_i):
                test_case["local_variables"].append(sorted_filelist[i].filename)
                count=count+1


# In[12]:


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


# In[13]:


with open("mutated_state",'wb') as f:
    pickle.dump(mutated_info,f)


# In[14]:


with open("comparison_info","rb") as f:
    comparison_info = pickle.load(f)


# In[15]:


# if "forbidden" not in comparison_info[test_class+" "+test_method+" "+str(i)]:


# In[16]:


# comparison_info[test_class+" "+test_method+" "+str(i)][state_index]= None


# In[17]:


# comparison_info[test_class+" "+test_method+" "+str(i)][state_index].add(element4.tag)


# In[18]:


zips_normal = zipfile.ZipFile("3-coverage-scan1"+"/"+zipfilepath)


# In[136]:


flag_para= False
index_para = 0
prev_test_class,prev_test_method=None,None


test_case_compare = 0
test_case_diff=0
mutation_compare=0
mutation_diff=0
flag_test_case_diff=False
flag_mutation_diff=False
my_diff=[]
for mutation in mutated_info:
    if mutation["status"]=="survive":
        flag_mutation_diff=False
        mutation_compare+=1
        flag_para=False
        index_para=0
        for test_case in mutation["test_cases"]:
            flag_test_case_diff=False
            test_case_compare+=1
            test_class = test_case["test_class"]
            test_method = test_case["test_method"]
            if(len(normal_state[test_class][test_method])>1):
                flag_para=True
                if(prev_test_class!=test_class or prev_test_method!=test_method):
                    index_para=0
            else:
                flag_para = False
                index_para=0
            normal_files = normal_state[test_class][test_method][index_para]["local_variables"]
            mutated_files = test_case["local_variables"]
            if "forbidden" in comparison_info[test_class+" "+test_method+" "+str(index_para)]:
                continue

            if(flag_para==False):

                if(len(normal_files)!=len(mutated_files)):
                    test_case_diff+=1
                    flag_test_case_diff=True
                    flag_mutation_diff=True
                    print("execution inconsistent")
                    continue
                for i in range(len(normal_files)):
                    if(flag_test_case_diff==False):
                        info = comparison_info[test_class+" "+test_method+" "+"0"]
                        if info!=None and i in info:
                            tag_bl=info[i]
                        else:
                            tag_bl=set()

                        normal_data=zips_normal.read(normal_files[i])
                        if not isinstance(normal_data,str):
                            normal_utf_8 = normal_data.decode("utf-8",errors="ignore")
                        else:
                            normal_utf_8 = normal_data
                        normal_root=ET.fromstring(normal_utf_8,parser=etree.XMLParser(recover=True))
                        normal_graph = get_graph_from_root(normal_root)
                        normal_all_elements = list(nx.bfs_tree(normal_graph, source=normal_root))                      

                        mutated_data=zips.read(mutated_files[i])
                        if not isinstance(mutated_data,str):
                            mutated_utf_8 = mutated_data.decode("utf-8",errors="ignore")
                        else:
                            mutated_utf_8= mutated_data
                        mutated_root=ET.fromstring(mutated_utf_8,parser=etree.XMLParser(recover=True))
                        mutated_graph = get_graph_from_root(mutated_root)
                        mutated_all_elements = list(nx.bfs_tree(mutated_graph, source=mutated_root))  

                        if len(mutated_all_elements)!=len(normal_all_elements):
#                             print("number of elements inconsistent")
                            test_case_diff+=1
                            flag_test_case_diff=True
                            flag_mutation_diff=True
                            break
                        else:
                            for e_index in range(len(normal_all_elements)):
                                element1 = mutated_all_elements[e_index]
                                element2 = normal_all_elements[e_index]
                                if element1.tag in tag_bl or element2.tag in tag_bl:
                                    pass
                                elif (not element1.tag==element2.tag) or (not element1.text==element2.text):
                                    test_case_diff+=1
                                    flag_test_case_diff=True
                                    flag_mutation_diff=True
                                    break
                if (flag_test_case_diff):
                    my_diff.append((normal_files,mutated_files))
                        
            else:
                diff_list=[False for x in range(len(normal_state[test_class][test_method]))]
                info = comparison_info[test_class+" "+test_method+" "+"0"]

                for j in range(len(normal_state[test_class][test_method])):
                    if "main_duration" not in normal_state[test_class][test_method][j]:
                        diff_list[j]=True
                        continue
                    normal_files = normal_state[test_class][test_method][j]["local_variables"]
                    for f in range(len(normal_files)):
                        if info!=None and f in info:
                            tag_bl=info[f]
                        else:
                            tag_bl=set()
                        normal_data=zips_normal.read(normal_files[f])
                        if not isinstance(normal_data,str):
                            normal_utf_8 = normal_data.decode("utf-8",errors="ignore")
                        else:
                            normal_utf_8 = normal_data
                        normal_root=ET.fromstring(normal_utf_8,parser=etree.XMLParser(recover=True))
                        normal_graph = get_graph_from_root(normal_root)
                        normal_all_elements = list(nx.bfs_tree(normal_graph, source=normal_root))                      

                        mutated_data=zips.read(mutated_files[f])
                        if not isinstance(mutated_data,str):
                            mutated_utf_8 = mutated_data.decode("utf-8",errors="ignore")
                        else:
                            mutated_utf_8= mutated_data
                        mutated_root=ET.fromstring(mutated_utf_8,parser=etree.XMLParser(recover=True))
                        mutated_graph = get_graph_from_root(mutated_root)
                        mutated_all_elements = list(nx.bfs_tree(mutated_graph, source=mutated_root))     
                        if len(normal_all_elements)!=len(mutated_all_elements):
                            diff_list[j]=True
                            continue
                        for e_index in range(len(normal_all_elements)):
                            element1 = mutated_all_elements[e_index]
                            element2 = normal_all_elements[e_index]
                            if element1.tag in tag_bl or element2.tag in tag_bl:
                                pass
                            elif (not element1.tag==element2.tag) or (not element1.text==element2.text):
                                diff_list[j]=True
                                break
                if sum(diff_list)==len(diff_list):
                    test_case_diff+=1
                    flag_test_case_diff=True
                    flag_mutation_diff=True
                        
                

            if(len(normal_state[test_class][test_method])>1):
                index_para=index_para+1
            prev_test_class=test_class
            prev_test_method=test_method
        if(flag_mutation_diff==True):
            mutation_diff+=1


# In[137]:


reports=[]
reports.append("*"*90)
reports.append("For survived mutants: ")
reports.append("Compare test case runs "+str(test_case_compare)+" times, "+str(test_case_diff)+"("+str(test_case_diff/test_case_compare)+")"+" are different")
reports.append("For "+str(mutation_compare)+" mutations, "+str(mutation_diff)+"("+str(mutation_diff/mutation_compare)+")"+"could have been killed")


# In[138]:


flag_para= False
index_para = 0
prev_test_class,prev_test_method=None,None


test_case_compare = 0
test_case_diff=0
mutation_compare=0
mutation_diff=0
flag_test_case_diff=False
flag_mutation_diff=False
my_diff=[]
for mutation in mutated_info:
    if mutation["status"]=="killed":
        flag_mutation_diff=False
        mutation_compare+=1
        flag_para=False
        index_para=0
        for test_case in mutation["test_cases"]:
            if test_case["state"]=="success":
                flag_test_case_diff=False
                test_case_compare+=1
                test_class = test_case["test_class"]
                test_method = test_case["test_method"]
                if(len(normal_state[test_class][test_method])>1):
                    flag_para=True
                    if(prev_test_class!=test_class or prev_test_method!=test_method):
                        index_para=0
                else:
                    flag_para = False
                    index_para=0
                normal_files = normal_state[test_class][test_method][index_para]["local_variables"]
                mutated_files = test_case["local_variables"]
                if "forbidden" in comparison_info[test_class+" "+test_method+" "+str(index_para)]:
                    continue

                if(flag_para==False):

                    if(len(normal_files)!=len(mutated_files)):
                        test_case_diff+=1
                        flag_test_case_diff=True
                        flag_mutation_diff=True
                        print("execution inconsistent")
                        continue
                    for i in range(len(normal_files)):
                        if(flag_test_case_diff==False):
                            info = comparison_info[test_class+" "+test_method+" "+"0"]
                            if info!=None and i in info:
                                tag_bl=info[i]
                            else:
                                tag_bl=set()

                            normal_data=zips_normal.read(normal_files[i])
                            if not isinstance(normal_data,str):
                                normal_utf_8 = normal_data.decode("utf-8",errors="ignore")
                            else:
                                normal_utf_8 = normal_data
                            normal_root=ET.fromstring(normal_utf_8,parser=etree.XMLParser(recover=True))
                            normal_graph = get_graph_from_root(normal_root)
                            normal_all_elements = list(nx.bfs_tree(normal_graph, source=normal_root))                      

                            mutated_data=zips.read(mutated_files[i])
                            if not isinstance(mutated_data,str):
                                mutated_utf_8 = mutated_data.decode("utf-8",errors="ignore")
                            else:
                                mutated_utf_8= mutated_data
                            mutated_root=ET.fromstring(mutated_utf_8,parser=etree.XMLParser(recover=True))
                            mutated_graph = get_graph_from_root(mutated_root)
                            mutated_all_elements = list(nx.bfs_tree(mutated_graph, source=mutated_root))  

                            if len(mutated_all_elements)!=len(normal_all_elements):
    #                             print("number of elements inconsistent")
                                test_case_diff+=1
                                flag_test_case_diff=True
                                flag_mutation_diff=True
                                break
                            else:
                                for e_index in range(len(normal_all_elements)):
                                    element1 = mutated_all_elements[e_index]
                                    element2 = normal_all_elements[e_index]
                                    if element1.tag in tag_bl or element2.tag in tag_bl:
                                        pass
                                    elif (not element1.tag==element2.tag) or (not element1.text==element2.text):
                                        test_case_diff+=1
                                        flag_test_case_diff=True
                                        flag_mutation_diff=True
                                        break
                    if (flag_test_case_diff):
                        my_diff.append((normal_files,mutated_files))

                else:
                    diff_list=[False for x in range(len(normal_state[test_class][test_method]))]
                    info = comparison_info[test_class+" "+test_method+" "+"0"]

                    for j in range(len(normal_state[test_class][test_method])):
                        if "main_duration" not in normal_state[test_class][test_method][j]:
                            diff_list[j]=True
                            continue
                        normal_files = normal_state[test_class][test_method][j]["local_variables"]
                        for f in range(len(normal_files)):
                            if info!=None and f in info:
                                tag_bl=info[f]
                            else:
                                tag_bl=set()
                            normal_data=zips_normal.read(normal_files[f])
                            if not isinstance(normal_data,str):
                                normal_utf_8 = normal_data.decode("utf-8",errors="ignore")
                            else:
                                normal_utf_8 = normal_data
                            normal_root=ET.fromstring(normal_utf_8,parser=etree.XMLParser(recover=True))
                            normal_graph = get_graph_from_root(normal_root)
                            normal_all_elements = list(nx.bfs_tree(normal_graph, source=normal_root))                      

                            mutated_data=zips.read(mutated_files[f])
                            if not isinstance(mutated_data,str):
                                mutated_utf_8 = mutated_data.decode("utf-8",errors="ignore")
                            else:
                                mutated_utf_8= mutated_data
                            mutated_root=ET.fromstring(mutated_utf_8,parser=etree.XMLParser(recover=True))
                            mutated_graph = get_graph_from_root(mutated_root)
                            mutated_all_elements = list(nx.bfs_tree(mutated_graph, source=mutated_root))     
                            if len(normal_all_elements)!=len(mutated_all_elements):
                                diff_list[j]=True
                                continue
                            for e_index in range(len(normal_all_elements)):
                                element1 = mutated_all_elements[e_index]
                                element2 = normal_all_elements[e_index]
                                if element1.tag in tag_bl or element2.tag in tag_bl:
                                    pass
                                elif (not element1.tag==element2.tag) or (not element1.text==element2.text):
                                    diff_list[j]=True
                                    break
                    if sum(diff_list)==len(diff_list):
                        test_case_diff+=1
                        flag_test_case_diff=True
                        flag_mutation_diff=True



                if(len(normal_state[test_class][test_method])>1):
                    index_para=index_para+1
                prev_test_class=test_class
                prev_test_method=test_method
        if(flag_mutation_diff==True):
            mutation_diff+=1


# In[139]:



reports.append("*"*90)
reports.append("For killed mutants: ")
reports.append("Compare passing test case runs "+str(test_case_compare)+" times, "+str(test_case_diff)+"("+str(test_case_diff/test_case_compare)+")"+" are different")


# In[119]:


# zips.extract(my_diff[36][1][0])
# zips_normal.extract(my_diff[36][0][0])


# In[140]:


flag_para= False
index_para = 0
prev_test_class,prev_test_method=None,None


test_case_compare = 0
test_case_diff=0
mutation_compare=0
mutation_diff=0
flag_test_case_diff=False
flag_mutation_diff=False
my_diff=[]
for mutation in mutated_info:
    if mutation["status"]=="killed" or mutation["status"]=="survive":
        flag_mutation_diff=False
        mutation_compare+=1
        flag_para=False
        index_para=0
        for test_case in mutation["test_cases"]:
            if test_case["state"]=="success":
                flag_test_case_diff=False
                test_case_compare+=1
                test_class = test_case["test_class"]
                test_method = test_case["test_method"]
                if(len(normal_state[test_class][test_method])>1):
                    flag_para=True
                    if(prev_test_class!=test_class or prev_test_method!=test_method):
                        index_para=0
                else:
                    flag_para = False
                    index_para=0
                normal_files = normal_state[test_class][test_method][index_para]["local_variables"]
                mutated_files = test_case["local_variables"]
                if "forbidden" in comparison_info[test_class+" "+test_method+" "+str(index_para)]:
                    continue

                if(flag_para==False):

                    if(len(normal_files)!=len(mutated_files)):
                        test_case_diff+=1
                        flag_test_case_diff=True
                        flag_mutation_diff=True
                        print("execution inconsistent")
                        continue
                    for i in range(len(normal_files)):
                        if(flag_test_case_diff==False):
                            info = comparison_info[test_class+" "+test_method+" "+"0"]
                            if info!=None and i in info:
                                tag_bl=info[i]
                            else:
                                tag_bl=set()

                            normal_data=zips_normal.read(normal_files[i])
                            if not isinstance(normal_data,str):
                                normal_utf_8 = normal_data.decode("utf-8",errors="ignore")
                            else:
                                normal_utf_8 = normal_data
                            normal_root=ET.fromstring(normal_utf_8,parser=etree.XMLParser(recover=True))
                            normal_graph = get_graph_from_root(normal_root)
                            normal_all_elements = list(nx.bfs_tree(normal_graph, source=normal_root))                      

                            mutated_data=zips.read(mutated_files[i])
                            if not isinstance(mutated_data,str):
                                mutated_utf_8 = mutated_data.decode("utf-8",errors="ignore")
                            else:
                                mutated_utf_8= mutated_data
                            mutated_root=ET.fromstring(mutated_utf_8,parser=etree.XMLParser(recover=True))
                            mutated_graph = get_graph_from_root(mutated_root)
                            mutated_all_elements = list(nx.bfs_tree(mutated_graph, source=mutated_root))  

                            if len(mutated_all_elements)!=len(normal_all_elements):
    #                             print("number of elements inconsistent")
                                test_case_diff+=1
                                flag_test_case_diff=True
                                flag_mutation_diff=True
                                break
                            else:
                                for e_index in range(len(normal_all_elements)):
                                    element1 = mutated_all_elements[e_index]
                                    element2 = normal_all_elements[e_index]
                                    if element1.tag in tag_bl or element2.tag in tag_bl:
                                        pass
                                    elif (not element1.tag==element2.tag) or (not element1.text==element2.text):
                                        test_case_diff+=1
                                        flag_test_case_diff=True
                                        flag_mutation_diff=True
                                        break
                    if (flag_test_case_diff):
                        my_diff.append((normal_files,mutated_files))

                else:
                    diff_list=[False for x in range(len(normal_state[test_class][test_method]))]
                    info = comparison_info[test_class+" "+test_method+" "+"0"]

                    for j in range(len(normal_state[test_class][test_method])):
                        if "main_duration" not in normal_state[test_class][test_method][j]:
                            diff_list[j]=True
                            continue
                        normal_files = normal_state[test_class][test_method][j]["local_variables"]
                        for f in range(len(normal_files)):
                            if info!=None and f in info:
                                tag_bl=info[f]
                            else:
                                tag_bl=set()
                            normal_data=zips_normal.read(normal_files[f])
                            if not isinstance(normal_data,str):
                                normal_utf_8 = normal_data.decode("utf-8",errors="ignore")
                            else:
                                normal_utf_8 = normal_data
                            normal_root=ET.fromstring(normal_utf_8,parser=etree.XMLParser(recover=True))
                            normal_graph = get_graph_from_root(normal_root)
                            normal_all_elements = list(nx.bfs_tree(normal_graph, source=normal_root))                      

                            mutated_data=zips.read(mutated_files[f])
                            if not isinstance(mutated_data,str):
                                mutated_utf_8 = mutated_data.decode("utf-8",errors="ignore")
                            else:
                                mutated_utf_8= mutated_data
                            mutated_root=ET.fromstring(mutated_utf_8,parser=etree.XMLParser(recover=True))
                            mutated_graph = get_graph_from_root(mutated_root)
                            mutated_all_elements = list(nx.bfs_tree(mutated_graph, source=mutated_root))     
                            if len(normal_all_elements)!=len(mutated_all_elements):
                                diff_list[j]=True
                                continue
                            for e_index in range(len(normal_all_elements)):
                                element1 = mutated_all_elements[e_index]
                                element2 = normal_all_elements[e_index]
                                if element1.tag in tag_bl or element2.tag in tag_bl:
                                    pass
                                elif (not element1.tag==element2.tag) or (not element1.text==element2.text):
                                    diff_list[j]=True
                                    break
                    if sum(diff_list)==len(diff_list):
                        test_case_diff+=1
                        flag_test_case_diff=True
                        flag_mutation_diff=True



                if(len(normal_state[test_class][test_method])>1):
                    index_para=index_para+1
                prev_test_class=test_class
                prev_test_method=test_method
        if(flag_mutation_diff==True):
            mutation_diff+=1


# In[141]:



reports.append("*"*90)
reports.append("For all passing test cases: ")
reports.append("Compare passing test case runs "+str(test_case_compare)+" times, "+str(test_case_diff)+"("+str(test_case_diff/test_case_compare)+")"+" are different")
for line in reports:
    print(line)


# In[142]:


with open('state_report.txt', 'w') as f:
    for line in reports:
        f.write(line)
        f.write('\n')

