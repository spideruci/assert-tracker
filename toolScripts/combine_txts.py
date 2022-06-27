#!/usr/bin/env python
# coding: utf-8

# In[9]:


import sys
import os
if __name__ =="__main__":
    files = sys.argv[1:-1]
    with open(sys.argv[-1], "w") as outfile:
        for filename in files:
            print(filename)
            with open(filename + os.sep+"target"+os.sep+"assertions.txt") as infile:
                contents = infile.read()
                outfile.write(contents)

