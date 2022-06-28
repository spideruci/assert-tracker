#!/usr/bin/env python
# coding: utf-8

# In[22]:


import zipfile as z
import os

zipfiles=[]
paths = os.listdir()
for path in paths:
    if os.path.isdir(path) and os.path.exists(path+os.path.sep+"target"+os.path.sep+"xmlOutput.zip"):
        zipfiles.append(path+os.path.sep+"target"+os.path.sep+"xmlOutput.zip")
print("zip files we are going to merge and move to target directory: ")
print(zipfiles)
print("-"*50)
with z.ZipFile("target"+os.path.sep+"xmlOutput.zip", 'w') as z1:
    for fname in zipfiles:
        zf = z.ZipFile(fname, 'r')
        for n in zf.namelist():
            z1.writestr(n, zf.open(n).read())
print("finished merging zip files")

