import docx
import re
import json
import matplotlib.pyplot as plt
dict={
        "姓名":"姓名",
        "有夫妻关系的人":"配偶",
        "出生时间":"公历出生日期",
        "出生地":"出生地",
        "死亡埋葬地":"死亡埋葬地",
        "文化程度":"文化程度",
        "父亲":"父亲",
        "享年":"享年",
        "谥号":"谥号",
        "公历去世时间":"公历过世日期"
        #"世":"世"
     }


def pre_data():
    content_pre=[]
    line1 = []
    doc = docx.Document("./源文件.docx")
    for para in doc.paragraphs:
        st=para.text.replace(" ","")
        if st!="":
            line1.append(st)
    #print(len(line1))
    with open("./output_zhou_deal_1.txt") as f:
        line2=f.readlines()
    # print(len(line2))
    # print(line2)
    # print(line2)
    pre_enty=0
    for i in range(len(line1)):
        dic={}
        #print(i)
        for value in dict.values():
            dic[value]=[]
        line2[i] = line2[i].replace("[","<")
        line2[i] = line2[i].replace("]", ">")
        # st=line2[i].split("，")[0]
        #
        # st=st.replace("<","")
        # st = st.replace(">", "")
        # st = st.replace("#", "")
        # st = "<"+st[:2]+"#姓名>"+st[2:]
        # if "系" in st:
        #     st1=st.split("系")[1]
        #     if len(st1)<=2:
        #         st = st.replace(st1, "<" + st1 + "#父亲>")
        #     elif st1[2] not in ["女","之","长","次"]:
        #         st = st.replace(st1[:3],"<"+st1[:3]+"#父亲>")
        #     else:
        #         st = st.replace(st1[:2], "<" + st1[:2] + "#父亲>")
        # line2[i]=st+"，"+line2[i].split("，")[1]
        #print(line2[i])
        match=re.findall("<.+?#.+?>",line2[i])
        for text in match:
            text=text[1:-2]
            enty=text.split("#")[0]
            label=text.split("#")[1]
            if label in dict.values():
                if dict[label]=="父亲" or dict[label]=="配偶":
                    dic["姓名"].append(enty)
                elif label=="文化程度":
                    enty=enty.replace("文化","")
                dic[dict[label]].append(enty)

        for key in dic.keys():
            dic[key]=list(set(dic[key]))
            pre_enty += len(dic[key])
        content_pre.append(dic)
    return content_pre,pre_enty

def real_data():
    content_real = []
    real_num={}
    real_enty=0
    with open("./out.json",encoding="utf8") as f:
        lis=json.load(f)
    content = []
    for i in range(len(lis)):
        dic = {}
        content.append(lis[i]["text"])
        for value in dict.values():
            dic[value] = []
        for j in range(len(lis[i]["entities"])):
            for key in lis[i]["entities"][j].keys():
                if key in dict.values():
                    dic[key].append(lis[i]["entities"][j][key])
        for key in dic.keys():
            dic[key]=list(set(dic[key]))
            if key in real_num.keys():
                real_num[key]+=len(dic[key])
            else:
                real_num[key]=len(dic[key])
            if key!="编号":
                real_enty+=len(dic[key])
        content_real.append(dic)
    #real_enty/=3
    return content_real,real_enty,content

def cacul():
    p_data,pre_enty=pre_data()
    r_data,real_enty=real_data()
    right_enty=0
    for i in range(len(p_data)):
        for key in p_data[i].keys():
            for enty in p_data[i][key] :
                if enty in r_data[i][key]:
                    right_enty+=1

    return pre_enty,real_enty,right_enty

def cacul_plot():
    p_data, pre_enty = pre_data()
    r_data, real_enty = real_data()
    P=[]
    R=[]
    F1=[]
    X=[]
    for i in range(len(p_data)):
        p = r = 0
        right_enty=0
        for key in p_data[i].keys():
            p+=len(p_data[i][key])
        for key in r_data[i].keys():
            r+=len(r_data[i][key])
        for key in p_data[i].keys():
            for enty in p_data[i][key] :
                if enty in r_data[i][key]:
                    right_enty+=1
        if p!=0:
            P.append(right_enty/p)
        else:
            P.append(0)
        if r!=0:
            R.append(right_enty/r)
        else:
            R.append(0)
        if (P[-1]+R[-1])!=0:
            print()
            F1.append(2*P[-1]*R[-1]/(P[-1]+R[-1]))
        else:
            F1.append(0)
        X.append(i)
    plt.plot(X,P,label="Pre")
    plt.plot(X,R,label="Rec")
    plt.plot(X,F1, label="F1")
    plt.legend()
    plt.show()

def cacul_select():
    p_data, _ = pre_data()
    r_data, _ ,content= real_data()
    pre_enty=real_enty=right_enty=0
    num = 0
    save_list=[]
    for i in range(len(p_data)):
        p = r = 0
        right=0
        for key in p_data[i].keys():
            p+=len(p_data[i][key])
        for key in r_data[i].keys():
            r+=len(r_data[i][key])
        for key in p_data[i].keys():
            for enty in p_data[i][key] :
                if enty in r_data[i][key]:
                    right+=1
        if  right!=0 and right/r>0.18:
            save_list.append(content[i])
            num+=1
            pre_enty+=p
            real_enty+=r
            right_enty+=right
    with open("./save-"+str(len(save_list))+".txt","w",encoding="utf8") as f:
        for i in save_list:
            f.write(i.replace("\n","")+"\n")
    print(num)
    return pre_enty, real_enty, right_enty

if __name__=="__main__":
    pre_enty,real_enty,right_enty=cacul_select()
    P = right_enty / pre_enty
    R = right_enty / real_enty
    F1=2*P*R/(P+R)
    print("Pre:",right_enty,"/",pre_enty," ",P)
    print("Rec:", right_enty, "/", real_enty, " ", R)
    print("F1:", F1)
    #cacul_plot()




