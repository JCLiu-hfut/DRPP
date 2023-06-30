import os
import re

sent=0
entity=0
class_=[]
for file in os.listdir("./"):
    if file.split(".")[-1] =="txt":
        with open(file,encoding="utf8")as f:
            for line in f.readlines():
                sent+=len(line.split("。"))
                line=line.replace("[","<")
                line=line.replace("]", ">")
                #print(line)
                match_list=re.findall("<.+?#.+?>",line)
                #print(match_list)
                entity+=len(match_list)
                for ent in match_list:
                    label=ent.split("#")[1][:-1]
                    if label not in class_:
                        class_.append(label)
print("sentence:",sent)
print("class:",len(class_))
print("entity:",entity)