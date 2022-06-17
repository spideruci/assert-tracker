#!/usr/bin/env python
# coding: utf-8

# In[468]:


import pickle
import matplotlib as mpl
import matplotlib.pyplot as plt
import numpy as np
import collections
import matplotlib
import re
import matplotlib.patches as mpatches


# In[ ]:





# In[469]:


with open("mutated_info","rb") as f:
    mutated_info=pickle.load(f)


# In[470]:


# with open("normal_state","rb") as f:
#     normal_state=pickle.load(f)


# In[471]:


reports = ["We only report covered mutants with killed or survived states"]
num_survive,num_killed,num_abnormal=0,0,0
for mutant in mutated_info:
    if mutant["status"]=="survive":
        num_survive+=1
    elif mutant["status"]=="killed":
        num_killed+=1
    elif mutant["status"]=="abnormal":
        num_abnormal+=1
reports.append("-"*40+"basic statistics"+"-"*40)
reports.append(str(num_survive)+" survive mutants; "+ str(num_killed)+" killed mutants (excluding"+str(num_abnormal)+" mutants killed due to memory error or timeout error)")
reports.append("Pitest reports test strength(excluding uncovered mutants)as " +"("+str(num_killed)+"+"+str(num_abnormal)+")"+                 "("+str(num_killed)+"+"+str(num_abnormal)+"+"+str(num_survive)+") = "+str((num_killed+num_abnormal)/(num_killed+num_abnormal+num_survive)) )
reports.append("-"*40 +"Crash Or Assertion Failure?"+"-"*40)         
reports.append("*"*90)


# In[472]:


num_success, num_crash, num_assert_failure=0,0,0
for mutant in mutated_info:
    if mutant["status"]=="killed":
        for test_case in mutant["test_cases"]:
            if test_case["state"]=="crash":
                num_crash=num_crash+1
            elif test_case["state"]=="assertion_failure":
                num_assert_failure+=1
            elif test_case["state"]=="success":
                num_success = num_success+1
reports.append("*"*90)
reports.append("How lucky are killed mutants? ")
reports.append("There are "+str(num_crash+num_assert_failure+num_success)+" test runs for killed mutants")
reports.append("Crash: "+ str(num_crash))
reports.append("Assertion failure: "+str(num_assert_failure))
reports.append("PASS: "+str(num_success))
reports.append("failing tests among all tests: "+str((num_crash+num_assert_failure)/(num_crash+num_assert_failure+num_success)))
reports.append("crash tests among failing tests: "+str((num_crash)/(num_crash+num_assert_failure)))


# In[473]:


for mutant in mutated_info:
    if mutant["status"]=="killed":
        num_success, num_crash,num_assert_failure = 0,0,0
        for test_case in mutant["test_cases"]:
            if test_case["state"]=="crash":
                num_crash=num_crash+1
            elif test_case["state"]=="assertion_failure":
                num_assert_failure+=1
            elif test_case["state"]=="success":
                num_success = num_success+1
        mutant["num_test_cases"]=num_crash+num_assert_failure+num_success
        if(mutant["num_test_cases"]==0):
            print("oh no")
        mutant["num_crash"]=num_crash
        mutant["num_assert_failure"]=num_assert_failure
        mutant["num_success"]=num_success
    elif mutant["status"]=="survive":
        mutant["num_success"]=len(mutant["test_cases"])        
        mutant["num_test_cases"]=mutant["num_success"]
        mutant["num_crash"]=0
        mutant["num_assert_failure"]=0


# In[474]:


reports.append("*"*90)
reports.append("Do killed mutants have more covering tests?")
num_tests_killed, num_tests_survive=0,0
num_killed,num_survive=0,0
for mutant in mutated_info:
    if mutant["status"]=="killed":
        num_crash = mutant["num_crash"]
        num_assert_failure = mutant["num_assert_failure"]
        num_success = mutant["num_success"]
        num_tests_killed= num_tests_killed+num_crash+num_assert_failure+num_success
        num_killed=num_killed+1
    elif mutant["status"]=="survive":
        num_tests_survive = num_tests_survive+num_crash+num_assert_failure+num_success
        num_survive = num_survive+1
reports.append("killed mutants have average " + str((num_tests_killed/num_killed)) +"tests per mutant ")
reports.append("survived mutants have average " + str((num_tests_survive/num_survive)) +"tests per mutant ")


# In[475]:


reports.append("*"*90)
reports.append("How are mutants killed?")


# In[476]:


# for mutant in mutated_info:
#     if mutant["status"]=="killed":
#         num_crash = mutant["num_crash"]
#         num_assert_failure = mutant["num_assert_failure"]
#         num_success = mutant["num_success"]
        
#         print((num_crash+num_assert_failure)/(num_crash+num_assert_failure+num_success))


# In[ ]:





# In[477]:


observed_mutants = [mutant for mutant in mutated_info if mutant["status"]=="killed"]
sorted_mutants = sorted(observed_mutants,key=lambda m:m["num_test_cases"])


# In[478]:


count_small=0
count_big=0
for mutant in sorted_mutants:
    if mutant["num_test_cases"]<=5:
        count_small=count_small+1
    else:
        count_big=count_big+1
limit = count_big/5

mutant_dict={"1":[],"2":[],"3":[],"4":[],"5":[]}
mutant_list = []

for index,mutant in enumerate(sorted_mutants):
    num= mutant["num_test_cases"]
    if num<=5:
        mutant_dict[str(num)].append(mutant)
    else:
        if len(mutant_list)<limit:
            mutant_list.append(mutant)
        else:
            if num==mutant_list[-1]["num_test_cases"]:
                mutant_list.append(mutant)
            else:
                mutant_dict[str(mutant_list[0]["num_test_cases"])+"-"+str(mutant_list[-1]["num_test_cases"])]=mutant_list
                mutant_list=[]
mutant_dict[str(mutant_list[0]["num_test_cases"])+"-"+str(mutant_list[-1]["num_test_cases"])]=mutant_list
                
        


# In[479]:


fig = plt.figure(figsize=(10,5),dpi=200)
ax1 = fig.add_subplot(111)
ax2 = ax1.twiny()
import matplotlib.ticker as mticker
for label, mutants in mutant_dict.items():
    num = len(mutants)
    all_crash, all_success, all_assert_failure=0,0,0
    for mutant in mutants:
        num_crash = mutant["num_crash"]
        num_success=mutant["num_success"]
        num_assert_failure = mutant["num_assert_failure"]
        all_crash = all_crash+num_crash
        all_success = all_success+num_success
        all_assert_failure = all_assert_failure+num_assert_failure
    all_num=all_success+all_crash+all_assert_failure
    ax1.bar(label, all_success/all_num,color="g")
    ax1.bar(label,all_crash/all_num,bottom=all_success/all_num,color='r')
    ax1.bar(label,all_assert_failure/all_num,bottom=(all_success+all_crash)/all_num,color='b')

#     break

ax2.set_xlim(ax1.get_xlim())
ax2.xaxis.set_major_locator(mticker.FixedLocator(ax1.get_xticks()))
x2_ticks=[len(m) for m in mutant_dict.values()]
ax2.set_xticklabels(x2_ticks)
ax1.set_xlabel("number of tests")
ax2.set_xlabel("number of mutants")

ax1.set_ylabel("proportion of test states")
ax1.set_yticks([0,1])
blue_patch = mpatches.Patch(color='blue', label='assertion failure')
green_patch = mpatches.Patch(color='green', label='pass')
red_patch = mpatches.Patch(color='red', label='crash')
plt.legend(handles=[blue_patch,green_patch,red_patch])
plt.savefig("pic1.png")
# fig.legend()


# In[480]:


fig = plt.figure(figsize=(10,5),dpi=200)
ax1 = fig.add_subplot(111)
ax2 = ax1.twiny()
import matplotlib.ticker as mticker
for label, mutants in mutant_dict.items():
    num = len(mutants)
    all_crash, all_success, all_assert_failure=0,0,0
    for mutant in mutants:
        num_crash = mutant["num_crash"]
        num_assert_failure = mutant["num_assert_failure"]
        all_crash = all_crash+num_crash
        all_assert_failure = all_assert_failure+num_assert_failure
    all_num=all_crash+all_assert_failure
    ax1.bar(label,all_crash/all_num,color='r')
    ax1.bar(label,all_assert_failure/all_num,bottom=all_crash/all_num,color='b')

#     break

ax2.set_xlim(ax1.get_xlim())
ax2.xaxis.set_major_locator(mticker.FixedLocator(ax1.get_xticks()))
x2_ticks=[len(m) for m in mutant_dict.values()]
ax2.set_xticklabels(x2_ticks)
ax1.set_xlabel("number of tests")
ax2.set_xlabel("number of mutants")

ax1.set_ylabel("proportion of test states")
ax1.set_yticks([0,1])
blue_patch = mpatches.Patch(color='blue', label='assertion failure')
red_patch = mpatches.Patch(color='red', label='crash')
plt.legend(handles=[blue_patch,green_patch,red_patch])
plt.savefig("pic2.png")
# fig.legend()


# In[481]:


reports.append("pic1")
reports.append("pic2: test failing reasons")


# In[ ]:





# In[482]:


reports.append("*"*90)
reports.append("Are there mutations that are once executed and failed?")


# In[483]:


count=0
count_all=0
for mutation in mutated_info:
    if(mutation["status"]=="killed") and mutation["num_test_cases"]>=5:
        if (mutation["num_crash"]==mutation["num_test_cases"]):
            count=count+1
    if(mutation["status"]!="abnormal"):
        count_all+=1
reports.append(str(count) +"/"+str(count_all)+ " mutants have at least 5 covering test cases, all of those test cases face crash!")


# In[484]:


count=0
count_all=0
for mutation in mutated_info:
    if(mutation["status"]=="killed") and mutation["num_test_cases"]>=5:
        if (mutation["num_crash"]+mutation["num_assert_failure"])/mutation["num_test_cases"]>=0.9:
            count=count+1
    if(mutation["status"]!="abnormal"):
        count_all+=1
reports.append(str(count) +"/"+str(count_all)+ " mutants have at least 5 covering test cases, at least 90% of those test cases face crash!")


# In[485]:


mutator_dict={}
for mutation in mutated_info:
    if mutation["status"]!="abnormal":
        mutator = mutation["mutation_info"]["mutator"].split(".")[-1]
        if mutator not in mutator_dict:
            mutator_dict[mutator]=[0,0]
        mutator_dict[mutator][0]+=1
        if (mutation["num_crash"]+mutation["num_assert_failure"])/mutation["num_test_cases"]>=0.9:
            mutator_dict[mutator][1]+=1
reports.append("*"*90)
reports.append("Have more at least 5 covering test cases and at least 90% of them fail with crash, distributed according to mutator")

for key, (a,b) in mutator_dict.items():
    reports.append(key+": "+str(b)+"/"+str(a))
    
reports.append("Keep an eye on nullReturnValue mutator! It's self explanatory!")


# In[486]:


mutator_dict={}
for mutation in mutated_info:
    if mutation["status"]!="abnormal":
        mutator = mutation["mutation_info"]["mutator"].split(".")[-1][:-7]
        if mutator not in mutator_dict:
            mutator_dict[mutator]=[]
        mutator_dict[mutator].append(mutation)


# In[500]:


fig = plt.figure(figsize=(10,5),dpi=200)
ax1 = fig.add_subplot(111)
plt.xticks(rotation=60)
ax2 = ax1.twiny()
all_nums=[]
for mutator, mutations in mutator_dict.items():
    all_crash, all_success, all_assert_failure=0,0,0
    for mutant in mutations:
        num_crash = mutant["num_crash"]
        num_success=mutant["num_success"]
        num_assert_failure = mutant["num_assert_failure"]
        all_crash = all_crash+num_crash
        all_success = all_success+num_success
        all_assert_failure = all_assert_failure+num_assert_failure
    all_num=all_success+all_crash+all_assert_failure
    all_nums.append(all_num)
    ax1.bar(mutator, all_success/all_num,color="g")
    ax1.bar(mutator,all_crash/all_num,bottom=all_success/all_num,color='r')
    ax1.bar(mutator,all_assert_failure/all_num,bottom=(all_success+all_crash)/all_num,color='b')
blue_patch = mpatches.Patch(color='blue', label='assertion failure')
red_patch = mpatches.Patch(color='red', label='crash')
plt.legend(handles=[blue_patch,green_patch,red_patch])


ax2.set_xlim(ax1.get_xlim())
ax2.xaxis.set_major_locator(mticker.FixedLocator(ax1.get_xticks()))
x2_ticks=all_nums
ax2.set_xticklabels(x2_ticks)
ax1.set_xlabel("category of mutation operator")
ax2.set_xlabel("number of test runs")

ax1.set_ylabel("proportion of test states")
ax1.set_yticks([0,1])
blue_patch = mpatches.Patch(color='blue', label='assertion failure')
green_patch = mpatches.Patch(color='green', label='pass')
plt.savefig("pic3.png",bbox_inches = 'tight')       


# In[488]:


reports.append("*"*90)
reports.append("test run states for different mutation operators!")
reports.append("pic3")


# In[499]:


with open('assertion_report.txt', 'w') as f:
    for line in reports:
        f.write(line)
        f.write('\n')

