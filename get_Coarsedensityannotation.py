import os
import openai
import re
def data_load(path):
    with open(path) as f:
        pass
OPENAI_API_KEY_LIST = ["API_key1","API-key2"]
index=0
count=len(OPENAI_API_KEY_LIST)
answer=["姓名","夫妻关系的人","出生日期","出生地点","死亡埋葬地","文化程度","父亲","享年","谥号","世或代","去世时间"]
#The folder where the files to be marked are stored
for filename in os.listdir("files2"):
    output_list=[]
    print(filename)
    if filename=="input.txt":
        with open("files2/"+filename,encoding="utf8") as f:
            for content in f.readlines():
                openai.api_key = os.getenv("OPENAI_API_KEY", OPENAI_API_KEY_LIST[index])
                index+=1
                index%=count

                output=content
                for i in answer:
                    prompt =content+"中的是"+i+"的有哪些？，一一罗列用、隔开"
                    response=openai.ChatCompletion.create(
                         model="gpt-3.5-turbo",
                         messages= [{"role": "user", "content": prompt}]
                    )

                    st=response['choices'][0]['message']['content']
                    #st=response.choices[0]["text"]
                    #print(i,":",st)
                    #st=st.split(":")
                    st=st.replace("\n","")
                    st=st.replace(" ","").split("、")
                    #print(i,":",st)
                    st1=[]
                    for j in st:
                        if "：" in j:
                            j=j.split("：")[1]
                        j=j.replace("。","")
                        j=j.replace("即可","")
                        j=j.replace("“","")
                        j = j.replace("“", "")
                        st1.append(j)
                        #print(j[1:])
                    #print(st1)
                    st = list(set(st1))
                    for j in st:
                        if j in output and j!="":
                            if i=="夫妻关系的人":
                                rep="["+j+"#"+"配偶]"
                            elif i=="世或代":
                                rep="[" + j + "#" + "世" + "]"
                            else:
                                rep = "[" + j + "#" + i + "]"
                            output=output.replace(j,rep)
                        elif len(j)>1 and j[1:] in output:
                            if i == "夫妻关系的人":
                                rep = "[" + j[1:] + "#" + "配偶]"
                            else:
                                rep = "[" + j[1:] + "#" + i + "]"
                            output = output.replace(j[1:], rep)
                print(output)
                output_list.append(output)
