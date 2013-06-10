#!/usr/bin/python
import re
import os

rootdir='/home/gagan/thesis/newstockquotes'
list = []

for subdir, dirs, files in os.walk(rootdir):
    for file in files:
        list.append(file)
        list.sort()

for i in range(len(list)):
    print(i, "=", list[i])
