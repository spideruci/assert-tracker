#!/usr/bin/env python
# coding: utf-8

# In[25]:


import sys
import re


#there is something wrong if assertions happen within beforeall
if __name__ == "__main__":
    count=0
    reportLines =[]
#     file_name = sys.argv[1]
    file_name = "2-assertion-scan/target/assertions.txt"
    with open(file_name,'r') as f:
        Lines = f.readlines();
    prev_state = "start"
    pre_testcase=None
    cur_testcase=None
    cur_state = None
    counter = 0
#     test_case_count=0
    for i in range(len(Lines)//2):
        num = int(Lines[2*i])
        text = Lines[2*i+1]
        if text.startswith("Constructor"):
            action = "Constructor"
        elif text.startswith("BeforeAll"):
            action = "BeforeAll"
        elif text.startswith("AfterAll"):
            action = "AfterAll"
        elif text.startswith("BeforeEach"):
            action = "BeforeEach"
            
        elif text.startswith("AfterEach"):
            action = "AfterEach"
        else:
            cur_testcase = text
            action = "testcase"
        cur_state = action

        if(cur_state=="Constructor" and prev_state!="start"):
            reportLines.append(str(counter)+" "+cur_testcase)
            counter=0
        counter=counter+num

        if(action=="testcase"):
            pre_testcase=cur_testcase
        prev_state = cur_state
    reportLines.append(str(counter)+" "+cur_testcase)

    for line in reportLines:

        num, _,_ = line.split(" ")
        if int(num)<=300:
        
            count =count+int(num)
    a_dict ={}

    with open('assert_count_report.txt', 'w') as f:
        f.write("test cases with assertions >= 300: \n")
        f.write("-"*30+"\n")
        large_assert_count = 0
        no_assert_count=0
        for line in reportLines:
            num, test_case, test_class = line.split(" ")
            if int(num)>=300:
                large_assert_count = large_assert_count+1
                f.write(str(num)+" "+ test_case+ " "+test_class)
        f.write("sum: "+str(large_assert_count)+"\n")
        f.write("-"*30+"\n")
        f.write("no assertion test cases: \n")
        f.write("-"*30+"\n")
        f.write("Note: some test cases have 'fail' statements not executed\n")
        f.write("-"*30+"\n")
        f.write(str(count)+" xml files would be dumped within one coverage run!\n")

        for line in reportLines:
            num, test_case, test_class = line.split(" ")
            if int(num)==0:
                no_assert_count = no_assert_count+1
                f.write(str(num)+" "+ test_case+ " "+test_class)
        f.write("sum: "+str(no_assert_count)+"\n")
        f.write("-"*30+"\n")

        f.write("individual results\n")
        if test_class[:-1] not in a_dict:
            a_dict[test_class[:-1]]=[test_case]
        else:
            a_dict[test_class[:-1]].append(test_case)

    with open('assert_count_report.txt', 'a') as f:
        for line in reportLines:
            f.write(line)


# In[ ]:




