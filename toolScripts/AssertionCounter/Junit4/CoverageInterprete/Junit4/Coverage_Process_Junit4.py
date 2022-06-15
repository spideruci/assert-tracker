#!/usr/bin/env python
# coding: utf-8

# In[84]:


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


# In[2]:


base1 = "3-coverage-scan1"
base2 = "3-coverage-scan2"
base3 = "3-coverage-scan3"
base4 = "3-coverage-scan4"
zipfilepath = 'target/xmlOutput.zip'


# In[77]:


with open(base1+"/target/test_info.txt", encoding='utf-16',errors = "ignore") as inf:
    Lines = inf.readlines()


# In[81]:


normal_state={}
current_test_class = None
current_test_method = None

current_test_case={}
current_states={}
count=0
prev=[False,False,False,False,False,False]
test_case_queue=[]
for line in Lines:
    
    before = len(re.findall("enter Before method",line))!=0
    after = len(re.findall("enter After method",line))!=0
    setUp= len(re.findall("Start executing outer test method: setUp",line))!=0
    tearDown = len(re.findall("Start executing outer test method: tearDown",line))!=0
    constructor = len(re.findall("Enter Junit4 Constructor",line))!=0 and len(re.findall("NotSpecified",line))==0
    exit_constructor = len(re.findall("exit Constructor Method",line))!=0
    # enter_method requires setUp and tearDown at first
    enter_method = len(re.findall("Start executing",line))!=0 and not setUp and not tearDown
    exit_tearDown = len(re.findall("Finish executing outer test method: tearDown",line))!=0
    exit_after = len(re.findall("exit After",line))!=0
    
    isAssertion = len(re.findall("Compiled at",line))!=0
    
    cur = (constructor, enter_method, before,after,setUp,tearDown)
    has_log = constructor or enter_method or before or after or setUp or tearDown

    
    #process
    
    if ((prev[1]==True or prev[5]==True or prev[3]==True) and constructor) or (prev[1]==True and cur[1]==True)            or (prev[1]==True and cur[4]==True) or ((prev[5]==True or prev[3]==True) and cur[1]==True)              or (prev[1]==True and cur[2]==True) or (prev[5]==True and cur[4]==True):
        test_case = test_case_queue.pop(0)
        if class_name not in normal_state:
            normal_state[class_name]={}
        if method_name not in normal_state[class_name]:
            normal_state[class_name][method_name]=[]
        normal_state[class_name][method_name].append(test_case)
        count=count+1
    
    
    cur_time = line.split(" ")[-1][:-1]
    if constructor:
        test_case_queue.append({"events":set()})
        c_start_time = cur_time
    elif exit_constructor:
        c_end_time = cur_time
        test_case_queue[-1]["constructor_time"]=(c_start_time,c_end_time)
        early_time=None
        late_time=None
    elif before:
        early_time = cur_time
        test_case_queue[0]["events"].add("before")
    elif setUp:
        early_time = cur_time
        test_case_queue[0]["events"].add("setUp")  
    elif after:
        test_case_queue[0]["events"].add("after")
    elif tearDown:
        test_case_queue[0]["events"].add("tearDown")
    elif exit_after:
        late_time = cur_time
        test_case_queue[0]["main_duration"]=(early_time,late_time)
    elif exit_tearDown:
        late_time = cur_time
        test_case_queue[0]["main_duration"]=(early_time,late_time)
    elif enter_method:
        if(early_time!=None):
            early_time =cur_time
        _,_,_,_,_,method_name,_,class_name,_ = line.split(" ")
        test_case_queue[0]["class"]=class_name
        test_case_queue[0]["method"]=method_name
        late_time = line.split(" ")[-1][:-1]
        pass

    #finish
    if has_log:
        prev=cur
        
test_case = test_case_queue.pop(0)
if class_name not in normal_state:
    normal_state[class_name]={}
if method_name not in normal_state[class_name]:
    normal_state[class_name][method_name]=[]
normal_state[class_name][method_name].append(test_case)
count=count+1
print("received "+ str(count)+" test cases, please verify the number reported here is consistent with other reports")


# In[85]:


# zips = zipfile.ZipFile(base1+"/"+zipfilepath)

