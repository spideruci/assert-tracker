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


# In[32]:


base = "4-mutation-scan"
def Lines():
    with open(base+"/target/test_result.txt", encoding='utf-8',errors = "ignore") as inf:
        for line in inf:
            yield line


# In[70]:


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


# In[274]:


normal_state={}
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
                                               "state":"unknown"})
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
#             continue
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


# In[ ]:


# 2550-201-2349(here we analyse, including killed/timedout/survive)



# 2349 =44 timeout+ 2021 killed+284 survived


# In[281]:


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

result1=[]
for mutation in mutated_info:
    if mutation["status"]=="killed":
        count=0
        for test_case in mutation["test_cases"]:
            if test_case["state"]!="success":
                count=count+1
        result1.append(count)
        
result2 = []
for line in Lines():
    detected_killed_info = re.findall("detected = KILLED by .*]",line)

    if len(detected_killed_info)!=0:
        mutation_info,test_cases = read_killed_mutation_detail(line)
        result2.append(len(test_cases))

        


# In[284]:


print("-"*20+"Below are only for debugging info"+"-"*20)
print()
print("if the following number is not equal, it indicates Pit's bugs: number of reported killing test runs")
print(sum(result1))
print(sum(result2))


# In[286]:


for mutation in mutated_info:
    for test_case in mutation["test_cases"]:
        count1=count1+1
        if test_case["state"]=="unknown":
            count=count+1
            print("Pay Attention: there are uninitialized test results!")


# In[287]:


with open("mutated_info","wb") as f:
    pickle.dump(mutated_info,f)

