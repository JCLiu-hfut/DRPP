#answer=["证书编号","获奖日期","指导老师","作品名称","团队成员"]["成立时间","公司名称","人名","作品公司类型","登记机关"]
#["姓名","有夫妻关系的人","出生时间","出生地点","死亡埋葬地","文化程度","父亲","享年","谥号","世或代","去世时间"]
import json
import os
import re
label_zhou=["出生地","文化程度","配偶","姓名","公历出生日期","父亲","享年"]
label_JS={"id":"证书编号",
"teachers":"指导老师",
"time":"获奖日期",
"project_name":"作品名称",
"students":"团队成员",
          }
label_YX=["药品名称","检测方法","不良反应","疾病","临床表现"]
label_KS=["成立时间","公司名称","登记机关","人名"]
def pre_data(filename):
    ruler={}
    with open("./rulers/"+filename,encoding="utf8") as f:
        for line in f.readlines():
           ruler[line.split("--")[0]]=line.split("--")[1].replace("\n","")
           ruler[line.split("--")[0]] = ruler[line.split("--")[0]].replace("n", "\n")
    #print(ruler)
    result=[]
    with open("./test_data_deal/"+filename,encoding="utf-8") as f:
        for line in f.readlines():
            result.append({})
            print(line)
            for r in ruler.keys():
                match_list=re.findall(ruler[r],"@"+line)
                #print(r,match_list)
                if r =="团队成员" and len(match_list)>0:
                    match_list=match_list[0].split("n")[-1].split(" ")
                if r not in result[-1].keys():
                    result[-1][r]=[]
                if r=="作品名称":
                    match_list = ["《"+i+"》" for i in match_list]
                for i in match_list:
                    if i!="":
                        if "、" in i:
                            for j in i.split("、"):
                                result[-1][r].append(j)
                        else:
                            result[-1][r].append(i)
    return result


def real_data(filename):
    real_list=[]
    if "zhou" in filename:
        text = []
        with open("./test_data_deal/"+filename,encoding="utf8") as f:
            for line in f.readlines():
                text.append(line.replace("\n",""))
        with open("./zhou.json",encoding="utf8") as f:
            data=json.load(f)
            for i in range(len(data)):
                if data[i]["text"].replace("\n","") in text:
                    real_list.append({})
                    for dic in data[i]["entities"]:
                        for key,value in dic.items():
                            if key in label_zhou:
                                if key not in real_list[-1].keys():
                                    real_list[-1][key]=[]
                                real_list[-1][key].append(value)
    elif filename=="KSGC.txt":
        with open("1.json",encoding="utf8") as f:
            data=json.load(f)
            for i in range(11,100):
                sign=0
                for j in data[i]["ret"]:
                    if j[0]=="DESC":
                        #print(j[1])
                        sign=1
                if sign==1:
                    real_list.append({})
                    for j in data[i]["ret"]:
                        if j[0] != "DESC" and j[0] in label_KS:
                            if j[0] not in real_list[-1].keys():
                                real_list[-1][j[0]]=[]
                            for k in j[1:]:
                                real_list[-1][j[0]].append(k)
                    if "name" in data[i].keys():
                        real_list[-1]["公司名称"]=[data[i]["name"]]
                    if "people" in data[i].keys():
                        real_list[-1]["人名"]=[data[i]["people"]]
                    if len(real_list)==10:
                        break
    elif "YX" in filename:
        #print(filename)
        with open("./biaozhu/" + filename,encoding="utf8") as f:
            for line in f.readlines():
                real_list.append({})
                line1=line.replace("[","<")
                line1 = line1.replace("]", ">")
                #print(line1)
                match_list = re.findall("<.+?#.+?>", line1)
                #print(match_list)
                for i in match_list:
                    key=i.split("#")[1][:-1]
                    vaule = i.split("#")[0][1:]
                    if key not in real_list[-1].keys():
                        real_list[-1][key]=[]
                    if "、" in vaule:
                        for j in vaule.split("、"):
                            real_list[-1][key].append(j)
                    else:
                        real_list[-1][key].append(vaule)
    else:
        with open("./total/"+filename.split(".")[0]+"/"+filename.replace("txt","json"),encoding="utf8") as f:
            data=json.load(f)
            for dic in data[10:20]:
                real_list.append({})
                for key in dic["data"].keys():
                    if key in label_JS.keys():
                        if label_JS[key] not in real_list[-1].keys():
                            real_list[-1][label_JS[key]]=[]
                        if key =="teachers" or key=="students":
                            for i in dic["data"][key]:
                                real_list[-1][label_JS[key]].append(i)
                        else:
                            real_list[-1][label_JS[key]].append(dic["data"][key])
    return real_list




def PRF(filename):
    pre=pre_data(filename)
    print((pre))
    real=real_data(filename)
    print(real)
    pre_count=real_count=right_count=0
    for i in pre:
        for key in i.keys():
            for j in i[key]:
                pre_count+=1
    for i in real:
        for key in i.keys():
            if key in label_zhou:
                real_count+=len(i[key])

    for i in range(len(pre)):
        for key in pre[i].keys():
            for j in pre[i][key]:
                if key in real[i].keys() and j in real[i][key]:
                    right_count+=1

    print("Pre:",right_count,"/",pre_count)
    print("Rec:", right_count, "/", real_count)
    print("F1:",2*(right_count)/(pre_count+real_count))

if __name__=="__main__":
    # for filename in os.listdir("./test_data"):
    #     if "001" in filename:
    #         print(filename)
    #         pre=pre_data(filename)
    #         #print("证书编号：20170321291n获奖证书n合肥工业大学n芮晨、蒲瓶、辛媱：n你们的作品《“猎鹰”—开创城市环境下的无人机防控》，在第七届安徽省“互联网+”大学生创新创业大赛中荣获高教主赛道银奖n指导老师：利畴n特发此证，以资鼓励。n安徽省教育厅n安徽省“互联网+大学生创新创业大赛组委会n二〇一七年三月")
    #         print("pre:",pre[0])
    #         real=real_data(filename)
    #         print("real:",real[0])
    PRF("YX-1.txt")
    PRF("YX-2.txt")
    PRF("YX-3.txt")
    PRF("zhou-1.txt")
    # PRF("YX-5.txt")
