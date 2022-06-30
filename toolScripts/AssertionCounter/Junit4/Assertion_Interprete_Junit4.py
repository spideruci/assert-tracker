# coding=utf-8
# @Time  : 6/11/2022 9:41 AM
# @File : Assertion_Interprete_Junit4.py
# @Software: PyCharm
import sys
import re
if __name__ == "__main__":
    reportLines =[]
    file_name = "2-assertion-scan/target/assertions.txt"
    with open(file_name,'r') as f:
        Lines = f.readlines();
    prev_state = "start"
    pre_testcase=None
    cur_testcase=None
    cur_state = None
    counter = 0
    test_case_count=0
    init_queue = []
    for i in range(len(Lines)//2):
        num = int(Lines[2*i])
        text = Lines[2*i+1]
        if text.startswith("<init>"):
            action = "init"
            # init_queue.append(num)
        elif text.startswith("tearDown"):
            action = "tearDown"
        elif text.startswith("setUp"):
            action = "setUp"
        elif text.startswith("Constructor"):
            action = "Constructor"
        elif text.startswith("Before"):
            action = "Before"
        elif text.startswith("After"):
            action = "After"
        else:
            test_case_count=test_case_count+1
            cur_testcase = text
            action = "testcase"

        cur_state = action
        if(cur_state=="testcase"):
            reportLines.append(str(counter) + " " + cur_testcase)
        # if((prev_state =="testcase" or prev_state == "tearDown")and (cur_state =="init" or cur_state =="setUp")):
        #     # init_count = init_queue.pop(0)
        #     #
        #     # reportLines.append(str(counter+init_count)+" "+cur_testcase)
        #
        #     counter = 0
        #
        # if(cur_state=="Constructor" and prev_state!="start"):
        #     reportLines.append(str(counter)+" "+cur_testcase)
        #
        # if(prev_state=="testcase" and cur_state=="testcase"):
        #     reportLines.append(str(counter)+" "+pre_testcase)
        #     counter=0
        #
        # if(action!="init"):
        #     counter = counter+num
        #
        # if(action=="testcase"):
        #     pre_testcase=cur_testcase
        # prev_state = cur_state

    # if len(init_queue)!=0:
    #     init_count = init_queue.pop(0)
    #     reportLines.append(str(counter+init_count)+" "+cur_testcase)
    # else:
    #     reportLines.append(str(counter)+" "+cur_testcase)

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
        for line in reportLines:
            num, test_case, test_class = line.split(" ")
            if int(num)==0:
                no_assert_count = no_assert_count+1
                f.write(str(num)+" "+ test_case+ " "+test_class)
        f.write("sum: "+str(no_assert_count)+"\n")
        f.write("-"*30+"\n")

        f.write("individual results\n")
        # if test_class[:-1] not in a_dict:
        #     a_dict[test_class[:-1]]=[test_case]
        # else:
        #     a_dict[test_class[:-1]].append(test_case)

    with open('assert_count_report.txt', 'a') as f:
        for line in reportLines:
            f.write(line)










