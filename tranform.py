import os
import re
supri={
    "父亲":0,
    "配偶":1,
    "出生时间":2,
    "出生地点":3,
    "死亡埋葬地":4,
    "文化程度":5,
    "死亡年龄":6,
    "谥号":7,
    "姓名":8,
    "享年":9,
    '去世时间':10,
    "世":11
}
for filename in os.listdir("../zhou"):
    if filename.split(".")[-1]!="py"and "output.txt" == filename:
        with open(filename,encoding="gb18030") as f:
            print(filename)
            line_list=[]
            for content in f.readlines():
                print(content)
                content = content.replace("[", "<")
                content = content.replace("]", ">")
                #content = content[1:]
                # print(content)
                if "<<<<" in content:
                    label_list = re.findall("<<<<.+?#.+?>#.+?>#.+?>#.+?>", content)
                    for label in label_list:
                        end = ""
                        enty = label.split("#")[0][4:]
                        if "。" in enty:
                            enty = enty.replace("。", "")
                            end = "。"
                        label1 = label.split("#")[1][:-1]
                        label2 = label.split("#")[2][:-1]
                        label3 = label.split("#")[3][:-1]
                        label4 = label.split("#")[4][:-1]
                        if supri[label1]==min(supri[label1],supri[label2],supri[label3],supri[label4]):
                            content = content.replace(label, "[" + enty + "#" + label1 + "]"+end)
                        elif supri[label2]==min(supri[label1],supri[label2],supri[label3],supri[label4]):
                            content = content.replace(label, "[" + enty + "#" + label2 + "]"+end)
                        elif supri[label3]==min(supri[label1],supri[label2],supri[label3],supri[label4]):
                            content = content.replace(label, "[" + enty + "#" + label3 + "]"+end)
                        elif supri[label4]==min(supri[label1],supri[label2],supri[label3],supri[label4]):
                            content = content.replace(label, "[" + enty + "#" + label4 + "]"+end)
                if "<<<" in content:
                    label_list = re.findall("<<<.+?#.+?>#.+?>#.+?>", content)
                    for label in label_list:
                        end = ""
                        enty = label.split("#")[0][3:]
                        if "。" in enty:
                            enty = enty.replace("。", "")
                            end = "。"
                        label1 = label.split("#")[1][:-1]
                        label2 = label.split("#")[2][:-1]
                        label3 = label.split("#")[3][:-1]
                        if supri[label1]==min(supri[label1],supri[label2],supri[label3]):
                            content = content.replace(label, "[" + enty + "#" + label1 + "]"+end)
                        elif supri[label2]==min(supri[label1],supri[label2],supri[label3]):
                            content = content.replace(label, "[" + enty + "#" + label2 + "]"+end)
                        elif supri[label3]==min(supri[label1],supri[label2],supri[label3]):
                            content = content.replace(label, "[" + enty + "#" + label3 + "]"+end)
                label_list = re.findall("<<.+?#.+?>#.+?>", content)
                for label in label_list:
                    # print(filename)
                    # print(label)
                    end=""
                    enty = label.split("#")[0][2:]
                    if "。" in enty:
                        #print(enty)
                        enty=enty.replace("。","")
                        end="。"
                    label1=label.split("#")[1][:-1]
                    label2=label.split("#")[2][:-1]
                    if supri[label1]<supri[label2]:
                        content=content.replace(label,"["+enty+"#"+label1+"]"+end)
                    else:
                        content=content.replace(label,"["+enty+"#"+label2+"]"+end)
                label_list = re.findall("<.+?。#.+?>", content)
                for label in label_list:
                    new_label=label.replace("。","")
                    content = content.replace(label, new_label+"。")
                content=content.replace("<","[")
                content = content.replace(">", "]")
                line_list.append(content)
        with open("./output_zhou_deal_1.txt","w") as f:
            for line in line_list:
                print(line)
                f.write(line)