import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




public class wordJiapuExtraction {

//    public static String[] rank_family = {"自", "汝", "绍", "国", "宏", "延", "祚", "昌", "克", "相", "盛", "时", "德", "显", "名", "彰", "忠", "正"};
    public static HashMap<String, String> pro = new HashMap<String, String>();//属性英文名-中文名
    public static HashMap<String, String> globalRules = new HashMap<String, String>();//全局规则库
//    public static HashMap<String, String> leftRules = new HashMap<String, String>();//局部规则库
//    public static HashMap<String, String> rightRules = new HashMap<String, String>();//局部规则库
    public static HashMap<String, String> localRules = new HashMap<String, String>();//局部规则库
    public static HashMap<String, String> finalRules = new HashMap<String, String>();//最终规则库
    //将数组的10修改为30
    public static String[] result = new String[10];//待标注数据
    public static HashMap<String, ArrayList<String>> proLeft = new HashMap<String, ArrayList<String>>();
    public static HashMap<String, ArrayList<String>> proRight = new HashMap<String, ArrayList<String>>();
    public static HashMap<String, ArrayList<String>> beginproLeft = new HashMap<String, ArrayList<String>>();
    public static HashMap<String, ArrayList<String>> beginproRight = new HashMap<String, ArrayList<String>>();
//    public static String shi="";
    public static String[] endFlag={"：","，","。"," ","；","、","（","）","【","】",":","(","　"," "};

    public static String solveFa(String father) {
        String fa = father.replace("公", "").replace("长", "").replace("次", "").replace("三", "").replace("四", "").replace("五", "").replace("六", "").replace("七", "").replace("八", "").replace("九", "").replace("子", "");
        return fa;
    }

    //写入局部规则
    public void write_local_rule(HashMap<String, String> content,String filename){
        try{
            String[] buff = filename.split("/");
            System.out.println("写入局部规则："+filename);
            String name=buff[buff.length-1];
            System.out.println("leng:"+buff.length);
            System.out.println("name:"+name);
            File file = new File("C:/N317/HPJiapuExtraction/src/main/knowledge/local_rules/"+name);

            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            for(String key:localRules.keySet()){
                bw.write(key+"--"+localRules.get(key)+'\n');
            }
            bw.close();
        }catch (IOException e){
            System.out.println("文件:  "+filename+"  写入错误");
        }
    }

    //获取文件夹下所有文件名
    public static List<String> getFiles(String path) {
        List<String> files = new ArrayList<String>();
        File file = new File(path);
        File[] tempList = file.listFiles();

        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
                files.add(tempList[i].toString());
                //文件名，不包含路径
                //String fileName = tempList[i].getName();
            }
            if (tempList[i].isDirectory()) {
                //这里就不递归了，
            }
        }
        return files;
    }

    //读取皇帝年号
    public ArrayList<String> readNianhao() throws Exception {
        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream("C:/N317/HPJiapuExtraction/src/main/knowledge/nianhao.txt"), "UTF-8"));
        String line = "";
        ArrayList<String> nianhao = new ArrayList<String>();
        while ((line = bufferedreader.readLine()) != null) {
            nianhao.add(line.replace("\n", ""));
        }
        return nianhao;
    }

    //读取相同字符关键词
    public HashMap<String,String> readSame() throws Exception {
        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream("C:/N317/HPJiapuExtraction/src/main/knowledge/same.txt"), "UTF-8"));
        String line = "";
        HashMap<String,String> same = new HashMap<String,String>();
        while ((line = bufferedreader.readLine()) != null) {
            String[] con=line.split("\t");
            same.put(con[0],con[1]);
        }
        return same;
    }
    /**
     * 读取家谱文件
     */
    private String ReadJiaPuFile(String filename) {
        String buffer = "";
        System.out.print("查看路径是否正确："+filename);

        // 判断文件类型（doc\docx\txt）
        if (filename.substring(filename.length() - 4).contains("txt")) {
            //读取txt类型家谱文件
            try {
                BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
                String line = "";
                while ((line = bufferedreader.readLine()) != null) {
                    buffer += line + "\n";

                }
            } catch (Exception e) {
                // TODO: handle exception
                System.out.println("txt家谱文件读取失败！");
            }
        } else {
            //读取doc\docx类型家谱文件
            try {
                if (filename.endsWith(".doc")) {
                    InputStream is = new FileInputStream(new File(filename));
                    WordExtractor ex = new WordExtractor(is);
                    buffer = ex.getText();
                    //ex.close();
                } else if (filename.endsWith(".docx")) {
                    FileInputStream fis = new FileInputStream(filename);
                    XWPFDocument xdoc = new XWPFDocument(fis);
                    XWPFWordExtractor extractor = new XWPFWordExtractor(xdoc);
                    buffer = extractor.getText();
                    //extractor.close();
                    fis.close();
                } else {
                    System.out.println("此文件不是word文件！");
                }
            } catch (Exception e) {
                System.out.println("word家谱文件读取失败！");
                e.printStackTrace();
            }
        }
        //System.out.println("输出家谱读取的源文件：\n"+buffer);
        return buffer;
    }

    /**
     * 写入文件
     **/

    public static void addExcel(String path, ArrayList<HashMap<String,String>> list) {

        try {
            WritableWorkbook wb = null;
            // 创建可写入的Excel工作簿
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            // 以fileName为文件名来创建一个Workbook
            wb = Workbook.createWorkbook(file);
            // 创建工作表
            WritableSheet ws = wb.createSheet("Sheet0", 0);
            // 要插入到的Excel表格的行号，默认从0开始
            Label labelId = new Label(0, 0, "编号");
            Label labelName = new Label(1, 0, "姓名");
            Label labelgender = new Label(2, 0, "性别");
            Label labelcourtesyName = new Label(3, 0, "字");
            Label labelpesudonym = new Label(4, 0, "号");
            Label labelfatherid = new Label(5, 0, "父亲编号");
            Label labelmotherid = new Label(6, 0, "母亲编号");
            Label labelpartnerid = new Label(7, 0, "配偶编号");
            Label labelshi = new Label(8, 0, "世");
            Label labelgenerition = new Label(9, 0, "辈份");
            Label labelfamilyrank = new Label(10, 0, "家庭排行");
            Label labeladdress = new Label(11, 0, "住址");
            Label labelera = new Label(12, 0, "朝代");
            Label labelBirthday = new Label(13, 0, "公历出生日期");
            Label labelChineseBirthday = new Label(14, 0, "农历出生日期");
            Label labeldeathdate = new Label(15, 0, "公历过世日期");
            Label labelChinesedeathdate = new Label(16, 0, "农历过世日期");
            Label labelyearsLived	 = new Label(17, 0, "享年");
            Label labelburied = new Label(18, 0, "葬于");
            Label labeleducation = new Label(19, 0, "文化程度");
            Label labelschool = new Label(20, 0, "毕业院校");
            Label labelworkunit = new Label(21, 0, "工作单位");
            Label labeloccupation = new Label(22, 0, "职业");
            Label labelduty = new Label(23, 0, "职务（官职）");
            Label labelbriefBiography = new Label(24, 0, "简介");
            Label labeloriginalFamily = new Label(25, 0, "祖籍");
            Label labelbirthPlace = new Label(26, 0, "出生地");
            Label labelethnicity = new Label(27, 0, "民族");
            Label labelposthumousTitle = new Label(28, 0, "谥号");
            Label labeldescription = new Label(29, 0, "其他描述");

            ws.addCell(labelId);
            ws.addCell(labelName);
            ws.addCell(labelfatherid);
            ws.addCell(labelmotherid);
            ws.addCell(labelpartnerid);
            ws.addCell(labelcourtesyName);
            ws.addCell(labelpesudonym);
            ws.addCell(labelgender);
            ws.addCell(labelChineseBirthday);
            ws.addCell(labelChinesedeathdate);
            ws.addCell(labelBirthday);
            ws.addCell(labeldeathdate);
            ws.addCell(labelburied);
            ws.addCell(labelfamilyrank);
            ws.addCell(labelshi);
            ws.addCell(labeladdress);
            ws.addCell(labelschool);
            ws.addCell(labelgenerition);
            ws.addCell(labelera);
            ws.addCell(labelyearsLived);
            ws.addCell(labeleducation);
            ws.addCell(labelworkunit);
            ws.addCell(labeloccupation);
            ws.addCell(labelduty);
            ws.addCell(labelbriefBiography);
            ws.addCell(labeloriginalFamily);
            ws.addCell(labelbirthPlace);
            ws.addCell(labelethnicity);
            ws.addCell(labelposthumousTitle);
            ws.addCell(labeldescription);
            for (int i = 0; i < list.size(); i++) {
                HashMap<String,String> p=list.get(i);
                Label labelId_i = new Label(0, i + 1, p.get("id") + "");
                Label labelName_i = new Label(1, i + 1, p.get("name"));
                Label labelsex_i = new Label(2, i + 1, p.get("sex"));
                if(p.containsKey("courtesy_name")){
                    Label labelcourtesyName_i = new Label(3, i + 1, p.get("courtesy_name"));
                    ws.addCell(labelcourtesyName_i);
                }
                else{
                    Label labelcourtesyName_i = new Label(3, i + 1, "");
                    ws.addCell(labelcourtesyName_i);
                }
                if(p.containsKey("pseudonym")){
                    Label labelpseudonym_i = new Label(4, i + 1, p.get("pseudonym"));
                    ws.addCell(labelpseudonym_i);
                }
                else{
                    Label labelpseudonym_i = new Label(4, i + 1, "");
                    ws.addCell(labelpseudonym_i);
                }

                if(p.containsKey("father_id")){
                    Label labelfatherid_i = new Label(5, i + 1, p.get("father_id"));
                    ws.addCell(labelfatherid_i);
                }
                else{
                    Label labelfatherid_i = new Label(5, i + 1, "");
                    ws.addCell(labelfatherid_i);
                }
                if(p.containsKey("mother_id")){
                    Label labelmotherid_i = new Label(6, i + 1, p.get("mother_id"));
                    ws.addCell(labelmotherid_i);
                }
                else{
                    Label labelmotherid_i = new Label(6, i + 1, "");
                    ws.addCell(labelmotherid_i);
                }
                if(p.containsKey("partner_id")){
                    Label labelpartnerid_i = new Label(7, i + 1, p.get("partner_id"));
                    ws.addCell(labelpartnerid_i);
                }
                else{
                    Label labelpartnerid_i = new Label(7, i + 1, "");
                    ws.addCell(labelpartnerid_i);
                }
                if(p.containsKey("shi")){
                    Label labelshi_i = new Label(8, i + 1, p.get("shi"));
                    ws.addCell(labelshi_i);
                }
                else{
                    Label labelshi_i = new Label(8, i + 1, "");
                    ws.addCell(labelshi_i);
                }
                if(p.containsKey("rank")){
                    Label labelrank_i = new Label(9, i + 1, p.get("rank"));
                    ws.addCell(labelrank_i);
                }
                else{
                    Label labelrank_i = new Label(9, i + 1, "");
                    ws.addCell(labelrank_i);
                }
                if(p.containsKey("rank_family")){
                    Label labelrank_family_i = new Label(10, i + 1, p.get("rank_family"));
                    ws.addCell(labelrank_family_i);
                }
                else{
                    Label labelrank_family_i = new Label(10, i + 1, "");
                    ws.addCell(labelrank_family_i);
                }
                if(p.containsKey("address")){
                    Label labeladdress_i = new Label(11, i + 1, p.get("address"));
                    ws.addCell(labeladdress_i);
                }
                else{
                    Label labeladdress_i = new Label(11, i + 1, "");
                    ws.addCell(labeladdress_i);
                }
                if(p.containsKey("era")){
                    Label labelera_i = new Label(12, i + 1, p.get("era"));
                    ws.addCell(labelera_i);
                }
                else{
                    Label labelera_i = new Label(12, i + 1, "");
                    ws.addCell(labelera_i);
                }
                if(p.containsKey("birth")){
                    Label labelbirth_i = new Label(13, i + 1, p.get("birth"));
                    ws.addCell(labelbirth_i);
                }
                else{
                    Label labelbirth_i = new Label(13, i + 1, "");
                    ws.addCell(labelbirth_i);
                }
                if(p.containsKey("china_birth")){
                    Label labelchina_birth_i = new Label(14, i + 1, p.get("china_birth"));
                    ws.addCell(labelchina_birth_i);
                }
                else{
                    Label labelchina_birth_i = new Label(14, i + 1, "");
                    ws.addCell(labelchina_birth_i);
                }
                if(p.containsKey("death_date")){
                    Label labeldeath_date_i = new Label(15, i + 1, p.get("death_date"));
                    ws.addCell(labeldeath_date_i);
                }
                else{
                    Label labeldeath_date_i = new Label(15, i + 1, "");
                    ws.addCell(labeldeath_date_i);
                }
                if(p.containsKey("china_death")){
                    Label labelchina_death_i = new Label(16, i + 1, p.get("china_death"));
                    ws.addCell(labelchina_death_i);
                }
                else{
                    Label labelchina_death_i = new Label(16, i + 1, "");
                    ws.addCell(labelchina_death_i);
                }
                if(p.containsKey("yearsLived")){
                    Label labelyearsLived_i = new Label(17, i + 1, p.get("yearsLived"));
                    ws.addCell(labelyearsLived_i);
                }
                else{
                    Label labelyearsLived_i = new Label(17, i + 1, "");
                    ws.addCell(labelyearsLived_i);
                }
                if(p.containsKey("grave")){
                    Label labelgrave_i = new Label(18, i + 1, p.get("grave"));
                    ws.addCell(labelgrave_i);
                }
                else{
                    Label labelgrave_i = new Label(18, i + 1, "");
                    ws.addCell(labelgrave_i);
                }
                if(p.containsKey("education")){
                    Label labeleducation_i = new Label(19, i + 1, p.get("education"));
                    ws.addCell(labeleducation_i);
                }
                else{
                    Label labeleducation_i = new Label(19, i + 1, "");
                    ws.addCell(labeleducation_i);
                }
                if(p.containsKey("school")){
                    Label labelschool_i = new Label(20, i + 1, p.get("school"));
                    ws.addCell(labelschool_i);
                }
                else{
                    Label labelschool_i = new Label(20, i + 1, "");
                    ws.addCell(labelschool_i);
                }
                if(p.containsKey("work_unit")){
                    Label labelwork_unit_i = new Label(21, i + 1, p.get("work_unit"));
                    ws.addCell(labelwork_unit_i);
                }
                else{
                    Label labelwork_unit_i = new Label(21, i + 1, "");
                    ws.addCell(labelwork_unit_i);
                }
                if(p.containsKey("occupation")){
                    Label labeloccupation_i = new Label(22, i + 1, p.get("occupation"));
                    ws.addCell(labeloccupation_i);
                }
                else{
                    Label labeloccupation_i = new Label(22, i + 1, "");
                    ws.addCell(labeloccupation_i);
                }
                if(p.containsKey("duty")){
                    Label labelduty_i = new Label(23, i + 1, p.get("duty"));
                    ws.addCell(labelduty_i);
                }
                else{
                    Label labelduty_i = new Label(23, i + 1, "");
                    ws.addCell(labelduty_i);
                }
                if(p.containsKey("briefBiography")){
                    Label labelbriefBiography_i = new Label(24, i + 1, p.get("briefBiography"));
                    ws.addCell(labelbriefBiography_i);
                }
                else{
                    Label labelbriefBiography_i = new Label(24, i + 1, "");
                    ws.addCell(labelbriefBiography_i);
                }
                if(p.containsKey("originalFamily")){
                    Label labeloriginalFamily_i = new Label(25, i + 1, p.get("originalFamily"));
                    ws.addCell(labeloriginalFamily_i);
                }
                else{
                    Label labeloriginalFamily_i = new Label(25, i + 1, "");
                    ws.addCell(labeloriginalFamily_i);
                }
                if(p.containsKey("birthPlace")){
                    Label labelbirthPlace_i = new Label(26, i + 1, p.get("birthPlace"));
                    ws.addCell(labelbirthPlace_i);
                }
                else{
                    Label labelbirthPlace_i = new Label(26, i + 1, "");
                    ws.addCell(labelbirthPlace_i);
                }
                if(p.containsKey("ethnicity")){
                    Label labelethnicity_i = new Label(27, i + 1, p.get("ethnicity"));
                    ws.addCell(labelethnicity_i);
                }
                else{
                    Label labelethnicity_i = new Label(27, i + 1, "");
                    ws.addCell(labelethnicity_i);
                }
                if(p.containsKey("posthumousTitle")){
                    Label labelposthumousTitle_i = new Label(28, i + 1, p.get("posthumousTitle"));
                    ws.addCell(labelposthumousTitle_i);
                }
                else{
                    Label labelposthumousTitle_i = new Label(28, i + 1, "");
                    ws.addCell(labelposthumousTitle_i);
                }
                if(p.containsKey("description")){
                    Label labeldescription_i = new Label(29, i + 1, p.get("description"));
                    ws.addCell(labeldescription_i);
                }
                else{
                    Label labeldescription_i = new Label(29, i + 1, "");
                    ws.addCell(labeldescription_i);
                }
                ws.addCell(labelId_i);
                ws.addCell(labelName_i);
                ws.addCell(labelsex_i);

            }
            // 写进文档
            wb.write();
            // 关闭Excel工作簿对象
            wb.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public int getIndex(String[] a, String b) {
        int i = 0;
        for (int j = 0; j < a.length; j++) {
            if (a[j].equals(b)) {
                i = j;
                break;
            }
        }
        return i;
    }

    //提取家庭排行被getFatherNameAndRank()函数调用
    public int getFamilyRank(String fartherAndRank) {
        int familyrank = 1;
        //int indexofRank = -1;
        if (fartherAndRank.contains("嗣子")) {
            familyrank = 1;
            //indexofRank = fartherAndRank.indexOf("嗣子");
        } else if (fartherAndRank.contains("公长子")||fartherAndRank.contains("公长女")) {
            familyrank = 1;
            //indexofRank = fartherAndRank.indexOf("公长子");
        } else if (fartherAndRank.contains("公次子")||fartherAndRank.contains("公次女")) {
            familyrank = 2;
            //indexofRank = fartherAndRank.indexOf("公次子");
        } else if (fartherAndRank.contains("公三子")||fartherAndRank.contains("公三女")) {
            familyrank = 3;
            //indexofRank = fartherAndRank.indexOf("公三子");
        } else if (fartherAndRank.contains("公四子")||fartherAndRank.contains("公四女")) {
            familyrank = 4;
            //indexofRank = fartherAndRank.indexOf("公四子");
        } else if (fartherAndRank.contains("公五子")||fartherAndRank.contains("公五女")) {
            familyrank = 5;
            //indexofRank = fartherAndRank.indexOf("公五子");
        } else if (fartherAndRank.contains("公六子")||fartherAndRank.contains("公六女")) {
            familyrank = 6;
            //indexofRank = fartherAndRank.indexOf("公六子");
        } else if (fartherAndRank.contains("公七子")||fartherAndRank.contains("公七女")) {
            familyrank = 7;
            //indexofRank = fartherAndRank.indexOf("公七子");
        } else if (fartherAndRank.contains("公八子")||fartherAndRank.contains("公八女")) {
            familyrank = 8;
            //indexofRank = fartherAndRank.indexOf("公八子");
        } else if (fartherAndRank.contains("之子") || fartherAndRank.contains("长子") || fartherAndRank.contains("公子")) {
            familyrank = 1;
            if (fartherAndRank.indexOf("之子") != -1) {
                //indexofRank = fartherAndRank.indexOf("之子");
            } else if (fartherAndRank.indexOf("长子") != -1) {
                //indexofRank = fartherAndRank.indexOf("长子");
            } else {
                //indexofRank = fartherAndRank.indexOf("公子");
            }
        } else if (fartherAndRank.contains("次子")||fartherAndRank.contains("次女")) {
            familyrank = 2;
            //indexofRank = fartherAndRank.indexOf("次子");
        } else if (fartherAndRank.contains("三子")||fartherAndRank.contains("三女")) {
            familyrank = 3;
            //indexofRank = fartherAndRank.indexOf("三子");
        } else if (fartherAndRank.contains("四子")||fartherAndRank.contains("四女")) {
            familyrank = 4;
            //indexofRank = fartherAndRank.indexOf("四子");
        } else if (fartherAndRank.contains("五子")||fartherAndRank.contains("五女")) {
            familyrank = 5;
            //indexofRank = fartherAndRank.indexOf("五子");
        } else if (fartherAndRank.contains("六子")||fartherAndRank.contains("六女")) {
            familyrank = 6;
            //indexofRank = fartherAndRank.indexOf("六子");
        } else if (fartherAndRank.contains("七子")||fartherAndRank.contains("七女")) {
            familyrank = 7;
            //indexofRank = fartherAndRank.indexOf("七子");
        } else if (fartherAndRank.contains("八子")||fartherAndRank.contains("八女")) {
            familyrank = 8;
            //indexofRank = fartherAndRank.indexOf("八子");
        } else if (fartherAndRank.contains("九子")||fartherAndRank.contains("九女")) {
            familyrank = 9;
            //indexofRank = fartherAndRank.indexOf("九子");
        } else if (fartherAndRank.contains("十子")||fartherAndRank.contains("十女")) {
            familyrank = 10;
            //indexofRank = fartherAndRank.indexOf("十子");
        } else {
            familyrank = 1;
            //indexofRank = fartherAndRank.length()-1;
        }
        return familyrank;
    }

    //读取属性英文名-中文名
    public void readProperty(String filename) throws Exception{

        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
        String line = "";
        while ((line = bufferedreader.readLine()) != null) {
            String[] con = line.split("\t");
//            System.out.println(con[0]);
            pro.put(con[1], con[0]);
        }

    }

    //读取全局规则库
    public void readGlobalRules(String filename) throws Exception{

        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
        String line = "";
        while ((line = bufferedreader.readLine()) != null) {
            String[] con = line.split("\t");
//            System.out.println(con[0]);
            globalRules.put(con[0], con[1]);
        }
    }
//C:\N317\HPJiapuExtraction\src\main\knowledge\output\output-yu.txt
    public void ceshiSelect() throws IOException {
        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream("C:/N317/HPJiapuExtraction/src/main/knowledge/output/output-zhou.txt"), "UTF-8"));
        String line = "";
        int num = 0;//行号
        while ((line = bufferedreader.readLine()) != null) {
            result[num] = line;
            num++;
        }
    }

    //随机挑选数据
    public void select(String[] text) throws IOException {

        int n = text.length;
        // 提取数量,构建随机不重复的数
        for (int i = 0; i < result.length; i++) {
            int r = (int) (Math.random() * n);
            if(!text[r].equals("")){
                result[i] = text[r];
                /*防止重复，去掉已经提取过的*/
                text[r] = text[n - 1];
                n--;
            }
            else{
                i--;
            }
        }

        // 提取结果C:\N317\HPJiapuExtraction\src\main\\knowledge\biaozhu2\0.txt
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("C:/N317/HPJiapuExtraction/src/main/knowledge/biaozhu2/0.txt")));
        for(int i=0;i<result.length;i++){
            String string = result[i];
            System.out.println(string);
            bw.write(string);
            bw.newLine();
        }
        bw.flush();
        bw.close();
    }

    //解析标注数据，构建局部规则库
    public void parse(String filename) throws IOException {
    //bufferedreader 已标注？
        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
        String line = "";
        int num = 0;//行号

        while ((line = bufferedreader.readLine()) != null) {
            //查看数据,该数据已标注？？？
            System.out.println("parse:查看数据  "+"num:"+num+" line:"+line);
            //起始特征
            boolean flag=false;//判断是否是一段话的起始特征
            if(line.substring(0,1).equals("^")){
                flag=true;
                line=line.substring(1,line.length());   // 4.3
            }
            Pattern pattern11 = Pattern.compile("\\[(.*?)\\]");
            Matcher matcher11 = pattern11.matcher(line);
            while (matcher11.find()) {
                System.out.println(matcher11.group(1));
                String valueKey = matcher11.group(1);
                String[] vk = valueKey.split("#");//vk[0]：属性值；vk[1]：属性名称
                if (vk[0].contains("*")) {//儿子姓名、女儿姓名、儿子、女儿
                    String newv = "";
                    String p = "";
                    Pattern pattern2 = Pattern.compile("\\*(.*?)@");
                    Matcher matcher2 = pattern2.matcher(vk[0]);
                    if (matcher2.find()) {
                        p = matcher2.group(1);//属性名称：女儿、儿子
                        String temp = "*" + matcher2.group(1);
                        newv = vk[0].replace("@", "").replace(temp, "");//属性值
                        //System.out.println("newv:="+newv);
                        if (result[num].contains(newv)) {
                            int begin = result[num].indexOf(newv);//属性值开始索引
                            int end = begin + newv.length();//属性值结束索引
                            String left = result[num].substring(0, begin);//属性值左边内容
                            String right = result[num].substring(end);//属性值右边内容
                            //System.out.println("result[num]: ="+result[num]);

                            if (proLeft.containsKey(vk[1])) {
                                ArrayList<String> l = proLeft.get(vk[1]);
                                l.add(left);
                                proLeft.put(vk[1], l);
                            } else {
                                ArrayList<String> l = new ArrayList<String>();
                                l.add(left);
                                proLeft.put(vk[1], l);
                            }
                            if (proRight.containsKey(vk[1])) {
                                ArrayList<String> l = proRight.get(vk[1]);
                                l.add(right);
                                proRight.put(vk[1], l);
                            } else {
                                ArrayList<String> l = new ArrayList<String>();
                                l.add(right);
                                proRight.put(vk[1], l);
                            }
                            //起始特征
                            if(flag){
                                flag=false;
                                if (beginproLeft.containsKey(vk[1])) {
                                    ArrayList<String> l = beginproLeft.get(vk[1]);
                                    l.add(left);
                                    beginproLeft.put(vk[1], l);
                                } else {
                                    ArrayList<String> l = new ArrayList<String>();
                                    l.add(left);
                                    beginproLeft.put(vk[1], l);
                                }
                                if (beginproRight.containsKey(vk[1])) {
                                    ArrayList<String> l = beginproRight.get(vk[1]);
                                    l.add(right);
                                    beginproRight.put(vk[1], l);
                                } else {
                                    ArrayList<String> l = new ArrayList<String>();
                                    l.add(right);
                                    beginproRight.put(vk[1], l);
                                }
                            }
                        }
                    }
                    //属性：儿子姓名、女儿姓名
                    String[] s1=vk[0].split("\\*");
                    for(int a=0;a<s1.length-1;a++){
                        String[] s2=s1[a].split("@");
                        String name=s2[s2.length-1];
                        if (newv.contains(name)) {
                            int begin = newv.indexOf(name);
                            int end = begin + name.length();
                            String left = newv.substring(0, begin);
                            String right = newv.substring(end);
                            if (proLeft.containsKey(p)) {
                                ArrayList<String> l = proLeft.get(p);
                                l.add(left);
                                proLeft.put(p, l);
                            } else {
                                ArrayList<String> l = new ArrayList<String>();
                                l.add(left);
                                proLeft.put(p, l);
                            }
                            if (proRight.containsKey(p)) {
                                ArrayList<String> l = proRight.get(p);
                                l.add(right);
                                proRight.put(p, l);
                            } else {
                                ArrayList<String> l = new ArrayList<String>();
                                l.add(right);
                                proRight.put(p, l);
                            }
                        }
                    }
                } else {
                    if (result[num].contains(vk[0])) {
                        int begin = result[num].indexOf(vk[0]);//属性值开始索引
                        int end = begin + vk[0].length();//属性值结束索引
                        String left = result[num].substring(0, begin);
                        String right = result[num].substring(end);
                        System.out.println("vk0"+vk[0]);
                        System.out.println("vk1"+vk[1]);
                        if (proLeft.containsKey(vk[1])) {
                            ArrayList<String> l = proLeft.get(vk[1]);
                            l.add(left);
                            proLeft.put(vk[1], l);
                        } else {
                            ArrayList<String> l = new ArrayList<String>();
                            l.add(left);
                            proLeft.put(vk[1], l);
                        }
                        if (proRight.containsKey(vk[1])) {
                            ArrayList<String> l = proRight.get(vk[1]);
                            l.add(right);
                            proRight.put(vk[1], l);
                        } else {
                            ArrayList<String> l = new ArrayList<String>();
                            l.add(right);
                            proRight.put(vk[1], l);
                        }
                        //起始特征
                        if(flag){
                            flag=false;
                            if (beginproLeft.containsKey(vk[1])) {
                                ArrayList<String> l = beginproLeft.get(vk[1]);
                                l.add(left);
                                beginproLeft.put(vk[1], l);
                            } else {
                                ArrayList<String> l = new ArrayList<String>();
                                l.add(left);
                                beginproLeft.put(vk[1], l);
                            }
                            if (beginproRight.containsKey(vk[1])) {
                                ArrayList<String> l = beginproRight.get(vk[1]);
                                l.add(right);
                                beginproRight.put(vk[1], l);
                            } else {
                                ArrayList<String> l = new ArrayList<String>();
                                l.add(right);
                                beginproRight.put(vk[1], l);
                            }
                        }
                    }
                }
            }
            num++;
        }
        for (String key : proLeft.keySet()) {
            ArrayList<String> l = proLeft.get(key);
            for (int j = 0; j < l.size(); j++) {
                System.out.println(key + "--" + l.get(j));
            }
        }
        System.out.println("局部规则库");
    }

    public boolean ifcontains(String[] a,String b){
        boolean flag=false;
        for(int i=0;i<a.length;i++){
            if(a[i].equals(b)){
                flag=true;
                break;
            }
        }
        return flag;
    }

    public HashMap<String,String> genLeftRules02(HashMap<String,ArrayList<String>> map) throws Exception {
        //中间数据
        HashMap<String, ArrayList<String>> leftRules_list = new HashMap<>();
        HashMap<String, ArrayList<Integer>> leftRules_list_num = new HashMap<>();
        //最终结果
        HashMap<String, String> leftRules = new HashMap<>();
        //关键词匹配集合
        HashMap<String, String> same = readSame();

        Set<String> set = map.keySet();
        for (String key : set) {      //遍历属性类型
            System.out.println("genLeftRules02 开始遍历规则： "+key);
            boolean flag1 = true;//是否是段首
            //对属性左边的内容进行初步地处理
            ArrayList<String> value = map.get(key);
            for (String s : value) {     //遍历每个属性的包含语句
                //个人感觉应有添加 4.5晚
                flag1=true;
                String L = "";
                if (s.length() > 0) {
                    //最左边的字符
                    String end = s.substring(s.length() - 1, s.length());
                    if (ifcontains(endFlag, end)) {//若左边最后一个字符是标点或空格     (导致【“出生日期”出问题的字段/code】)
                        L = end;
                        //从左边倒数第二的字符开始遍历
                        for (int index = s.length() - 2; index >= 0; index--) {
                            String w = s.substring(index, index + 1);
                            //该字符不是标点或空格
                            if (!ifcontains(endFlag, w)) {
                                //填入L
                                L = w + L;
                            } else {
                                break;
                            }
                        }
                    } else { ////若左边最后一个字符不是标点或空格,直接从最左边开始遍历
                        for (int index = s.length() - 1; index >= 0; index--) {
                            String w = s.substring(index, index + 1);
                            //没遇到标点就填入该字符
                            if (!ifcontains(endFlag, w)) {
                                L = w + L;
                            } else{
                                break;
                            }
                        }
                    }
                    System.out.println("genLeftRules02 中的  L ="+L);
                    //属性左边的字符已提取完成，并保存在L中，下面开始自动生成规则
                    String leftRule = "";
                    if (L.length() > 0) {  //判断是否为段首
                        flag1 = false;
                    }
                    //开始进入修改code
                    if(leftRules_list.containsKey(key)){ //判断之前已存入对应该key的规则集
                        ArrayList<String> lrule_list = leftRules_list.get(key); //将之前存入的对应该key的规则数组取出
                        //判定循环中是否由之前的规则能符合当下规则
                        int if_endlist=0;
                        //循环遍历对应key的之前的规则集
                        for(int yi=0;yi<lrule_list.size();yi++){
                            //每次规则集的循环都重新开始迭代当前规则
                            leftRule = "";
                            //lr放着之前的每一条规则的字符数组
                            String[] lr = lrule_list.get(yi).split("#");
                            System.out.println("leftRule02 String[] lr ="+Arrays.toString(lr));
                            boolean flag = false;//判断是否是特殊属性（父亲、儿子、女儿）
                            if (key.equals("父亲") || key.equals("儿子") || key.equals("女儿")) {
                                flag = true;
                            }
                            int index=L.length()-1;
                            //比较的循环？匹配子串
                            for (int i = lr.length - 1; i >= 0; i--) {
                                if(index<0){
                                    break;
                                }
                                String l = L.substring(index, index + 1);
                                index--;
                                //比较L（当前轮次）的最左字符和上一轮规则的最左字符是否相同
                                if (l.equals(lr[i])) {
                                    leftRule = l + "#" + leftRule;
                                } else if (same.containsKey(key) && (same.get(key).contains(l) && same.get(key).contains(lr[i]))) {
                                    leftRule = same.get(key) + "#" + leftRule;
                                } else if (ifcontains(endFlag, l)) {//判断是否是标点或空格
                                    if (lr[i].contains(",")) {
                                        String[] t = lr[i].split(",");
                                        if (ifcontains(endFlag, t[0])) {
                                            if(!lr[i].contains(l)){
                                                leftRule = l + "," + lr[i] + "#" + leftRule;
                                            }
                                            else {
                                                leftRule = lr[i] + "#" + leftRule;
                                            }
                                        }
                                    } else {
                                        if (ifcontains(endFlag, lr[i])) {
                                            if(!lr[i].contains(l)){
                                                leftRule = l + "," + lr[i] + "#" + leftRule;
                                            }
                                            else {
                                                leftRule = lr[i] + "#" + leftRule;
                                            }
                                        }
                                    }
                                } else if(flag){    //是父亲、儿子、女儿等特殊属性
                                    if (l.equals("子")) {
                                        if (lr[i].equals("女")) {
                                            leftRule = "子,女" + "#" + leftRule;
                                        } else if (lr[i].equals("子,女")) {
                                            leftRule = "子,女" + "#" + leftRule;
                                        }
                                    }
                                    if (l.equals("女")) {
                                        if (lr[i].equals("子")) {
                                            leftRule = "子,女" + "#" + leftRule;
                                        } else if (lr[i].equals("子,女")) {
                                            leftRule = "子,女" + "#" + leftRule;
                                        }
                                    }
                                } else {
                                    break;
                                }
                            }
                            //
                            if (leftRule.length()>0&&leftRule.substring(leftRule.length()-1,leftRule.length()).equals("#")){
                                leftRule=leftRule.substring(0,leftRule.length()-1);
                            }
                            // 个人感觉以上为本阶段规则生成代码
                            // 现有想法是这里做判断：
                            //  1.如果规则与之前的第i条规则相重合，则，该规则取代原有的第i条规则入库
                            //  2.如果规则与之前的所有规则都不相符合，则，直接将最左的所有字符作为新规则入库
                            if(leftRule.length()>0){    ///(1.)
                                //替换之前的值
                                lrule_list.set(yi,leftRule);
                                System.out.println("leftRule - l(1.): "+leftRule);
                                leftRules_list.put(key,lrule_list);
                                if_endlist=1;
                                int bef_num = leftRules_list_num.get(key).get(yi) + 1;//取之前的次数+1
                                leftRules_list_num.get(key).set(yi,bef_num);
                                break;
                            }
                        }
                        if (if_endlist==0){   ///(2.)
//                            lrule_list.add(L);
                            for(int index3=0;index3 < L.length();index3++){
                                leftRule = leftRule + L.substring(index3, index3 + 1) + "#";
                            }
                            if(L.length() > 0){
                                leftRule = leftRule.substring(0, leftRule.length() - 1);  //这条语句用于删去最后的"#"
                                lrule_list.add(leftRule);
                                leftRules_list.put(key,lrule_list);
                                System.out.println("leftRule - l:(2.) "+L);
                                leftRules_list_num.get(key).add(0);
                            }

                        }

                    } else { //判断之前未存入对应该key的规则集
                        for(int index3=0;index3 < L.length();index3++){
                            leftRule = leftRule + L.substring(index3, index3 + 1) + "#";
                        }
                        if(L.length() > 0){
                            leftRule = leftRule.substring(0, leftRule.length() - 1);  //这条语句用于删去最后的"#"
                            ArrayList<String> l = new ArrayList<String>();
                            l.add(leftRule);
                            System.out.println("leftRule - l(3.): "+leftRule);
                            leftRules_list.put(key,l);
                            ArrayList<Integer> l_num =new ArrayList<Integer>();
                            l_num.add(0);
                            leftRules_list_num.put(key,l_num);
                        }
                    }
                }
                if(flag1){
                    //纵然是段首，也要判断是否已经写入规则库
                    if(leftRules_list.containsKey(key)){
                        ArrayList<String> lrule_list1 = leftRules_list.get(key);
                        int if_find=0;
                        for(int i1=0;i1<lrule_list1.size();i1++){
                            if (lrule_list1.get(i1)=="^"){
                                int bef_num = leftRules_list_num.get(key).get(i1) + 1;
                                leftRules_list_num.get(key).set(i1,bef_num);
                                if_find=1;
                            }
                        }
                        if(if_find==0){
//                            ArrayList<String> l = new ArrayList<String>();
                            lrule_list1.add("^");
                            leftRules_list.put(key, lrule_list1);
                            ArrayList<Integer> l_num =leftRules_list_num.get(key);
                            l_num.add(0);
                            leftRules_list_num.put(key, l_num);
                            System.out.println("flag1        key :" + key + "   l:" + lrule_list1 + "   l_num" + l_num);
                        }

                    }else {
                        ArrayList<String> l = new ArrayList<String>();
                        l.add("^");
                        leftRules_list.put(key, l);
                        ArrayList<Integer> l_num = new ArrayList<Integer>();
                        l_num.add(0);
                        leftRules_list_num.put(key, l_num);
                        System.out.println("flag1        key :" + key + "   l:" + l + "   l_num" + l_num);
                    }
                }
            }

        }
        //开始重新处理规则
        //按照统计原理每个key的规则取最大数的规则
        System.out.println("\n\ngenLeftRules02 最终规则查验：   ");
        for(String key:leftRules_list.keySet()){
            System.out.println(key+"  "+leftRules_list.get(key));
            System.out.println(key+"  "+leftRules_list_num.get(key));
            ArrayList<String> rul_list=leftRules_list.get(key);
            ArrayList<Integer> rul_num=leftRules_list_num.get(key);
            int kk=rul_num.get(0);
            int kn=0;
            for(int i=0;i<rul_list.size();i++){
                if(rul_num.get(i)>kk){
                    kk=rul_num.get(i);
                    kn=i;
                }
            }
            //将最终结果收入最终规则集中
            String finalRule="";
            String[] per=rul_list.get(kn).split("#");
            for(int i=0;i<per.length;i++){
                if(per[i].contains(",")){
                    finalRule=finalRule+"["+per[i]+"]";
                }
                else{
                    finalRule=finalRule+per[i];
                }
            }
            System.out.println(key+"--"+finalRule);
            leftRules.put(key,finalRule);
        }



        return leftRules;
    }




    public HashMap<String,String> genLeftRules(HashMap<String,ArrayList<String>> map) throws Exception {
        System.out.println("begin left rules......");
        //新建一个存储函数 leftRules_list用列表的方式存储所有值
        HashMap<String,ArrayList<String>> leftRules_list=new HashMap<>();
        HashMap<String,ArrayList<Integer>> leftRules_list_num =new HashMap<>(); //sum

        HashMap<String,String> leftRules=new HashMap<>();
        HashMap<String,String> same=readSame();

        Set<String> set =map.keySet();
        for(String key : set) {//对每一个属性进行处理
            boolean flag1=true;//是否是段首
            System.out.println(key);
//            if (key.equals("儿子") || key.equals("女儿")) {
                //对属性左边的内容进行初步地处理
                ArrayList<String> value = map.get(key);
                for (String s : value) {
                   System.out.println("left-s:"+s);
                    //对属性的每一个左边内容进行处理
                    String L = "";
                    if(s.length()>0) {
                        String end = s.substring(s.length() - 1, s.length());
                        if (ifcontains(endFlag, end)) {//若左边最后一个字符是标点或空格
                            L = end;
                            for (int index = s.length() - 2; index >= 0; index--) {
                                String w = s.substring(index, index + 1);
                                if (!ifcontains(endFlag, w)) {
                                    L = w + L;
                                } else {
                                    break;
                                }
                                System.out.println("left-w:"+w);
                            }
                        } else {//若左边最后一个字符不是标点或空格
                            for (int index = s.length() - 1; index >= 0; index--) {
                                String w = s.substring(index, index + 1);
                                if (!ifcontains(endFlag, w)) {
                                    L = w + L;
                                } else {
                                    break;
                                }
                                System.out.println("left-w:"+w);
                            }
                        }
                        System.out.println("=L="+L);
                        //自动生成规则
                        String leftRule = "";
                        if (L.length() > 0) {
                            flag1 = false;
                        }
                        if (leftRules.containsKey(key)) {
                            String lrule = leftRules.get(key);//前一个语句匹配的规则
                            System.out.println("key :="+key);
                            String[] lr = lrule.split("#");
                            System.out.println("lr :="+lrule);
                            boolean flag = false;//判断是否是特殊属性（父亲、儿子、女儿）
                            if (key.equals("父亲") || key.equals("儿子") || key.equals("女儿")) {
                                flag = true;
                            }
//                            int index = lr.length > L.length() ? L.length() : lr.length;
                            int index=L.length()-1;
                            //匹配子串     4.4 刘佳成
                            for (int i = lr.length - 1; i >= 0; i--) {
                                if(index<0){
                                    break;
                                }
                                String l = L.substring(index, index + 1);
                                System.out.println("=l="+l);
                                System.out.println("=lr="+lr[i]);
                                System.out.println(same.get(key));
                                index--;
                                if (l.equals(lr[i])) {
                                    leftRule = l + "#" + leftRule;
                                } else if (same.containsKey(key) && (same.get(key).contains(l) && same.get(key).contains(lr[i]))) {
                                    leftRule = same.get(key) + "#" + leftRule;
                                } else if (ifcontains(endFlag, l)) {//判断是否是标点或空格
                                    if (lr[i].contains(",")) {
                                        String[] t = lr[i].split(",");
                                        if (ifcontains(endFlag, t[0])) {
                                            if(!lr[i].contains(l)){
                                                leftRule = l + "," + lr[i] + "#" + leftRule;
                                            }
                                            else{
                                                leftRule = lr[i] + "#" + leftRule;
                                            }
                                        }
                                    } else {
                                        if (ifcontains(endFlag, lr[i])) {
                                            if(!lr[i].contains(l)){
                                                leftRule = l + "," + lr[i] + "#" + leftRule;
                                            }
                                            else{
                                                leftRule = lr[i] + "#" + leftRule;
                                            }
                                        }
                                    }
                                } else if (flag) {
                                    if (l.equals("子")) {
                                        if (lr[i].equals("女")) {
                                            leftRule = "子,女" + "#" + leftRule;
                                        } else if (lr[i].equals("子,女")) {
                                            leftRule = "子,女" + "#" + leftRule;
                                        }
                                    }
                                    if (l.equals("女")) {
                                        if (lr[i].equals("子")) {
                                            leftRule = "子,女" + "#" + leftRule;
                                        } else if (lr[i].equals("子,女")) {
                                            leftRule = "子,女" + "#" + leftRule;
                                        }
                                    }
                                } else {
                                    break;
                                }
                            }
                            if(leftRule.length()>0&&leftRule.substring(leftRule.length()-1,leftRule.length()).equals("#")){
                                leftRule=leftRule.substring(0,leftRule.length()-1);
                            }
//                            System.out.println(key+"--"+leftRule);
                            leftRules.put(key, leftRule);

                            //刘佳成 4.4    判断是否有该key
                            if (leftRules_list.containsKey(key)) {
                                ArrayList<String> l = leftRules_list.get(key);//拿上一次的数据
                                System.out.println("leftRules_list-ll:="+l);
                                l.add(leftRule);
                                leftRules_list.put(key, l);
                            } else {
                                ArrayList<String> l = new ArrayList<String>();
                                l.add(leftRule);
                                leftRules_list.put(key,l);
                            }

                        } else {
                            for (int index3 = 0; index3 < L.length(); index3++) {
                                leftRule = leftRule + L.substring(index3, index3 + 1) + "#";
                            }
                            if (L.length() > 0) {
                                leftRule = leftRule.substring(0, leftRule.length() - 1);
                                leftRules.put(key, leftRule);

                                //刘佳成 4.4    判断是否有该key
                                if (leftRules_list.containsKey(key)) {
                                    ArrayList<String> l = leftRules_list.get(key);//拿上一次的数据
                                    l.add(leftRule);
                                    leftRules_list.put(key, l);
                                } else {
                                    ArrayList<String> l = new ArrayList<String>();
                                    l.add(leftRule);
                                    leftRules_list.put(key,l);
                                }
                            }
                        }
                    }
                }
                if(flag1){
                    leftRules.put(key,"^");

                    //刘佳成 4.4    判断是否有该key
                    if (leftRules_list.containsKey(key)) {
                        ArrayList<String> l = leftRules_list.get(key);//拿上一次的数据
                        l.add("^");
                        leftRules_list.put(key, l);
                    } else {
                        ArrayList<String> l = new ArrayList<String>();
                        l.add("^");
                        leftRules_list.put(key,l);
                    }
                }
//            }

        }

        //查看leftRules_list中的数据
        System.out.println("\nleftRules_list: ");
        for(String key:leftRules_list.keySet()){
            System.out.println(key+"  "+leftRules_list.get(key));
        }

        //对规则重新整理
        System.out.println("对规则重新整理");
        for(String key:leftRules.keySet()){
            String value=leftRules.get(key);
            System.out.println("leftRules-value :="+value);
            String[] per=value.split("#");
            String finalRule="";
            for(int i=0;i<per.length;i++){
                if(per[i].contains(",")){
                    finalRule=finalRule+"["+per[i]+"]";
                }
                else{
                    finalRule=finalRule+per[i];
                }
            }
            //该处显现结果：搜索[left_finalRule: 姓名]
            System.out.println("left_finalRule: "+key+"--"+finalRule);
            leftRules.put(key,finalRule);
        }
        return leftRules;
    }


    public HashMap<String,String> genRightRules02(HashMap<String,ArrayList<String>> map) throws Exception {
        //中间数据
        HashMap<String, ArrayList<String>> rightRules_list = new HashMap<>();
        HashMap<String, ArrayList<Integer>> rightRules_list_num = new HashMap<>();
        //最终结果
        HashMap<String, String> rightRules = new HashMap<>();
        //关键词匹配集合
        HashMap<String, String> same = readSame();

        Set<String> set = map.keySet();
        for(String key : set) {     //遍历属性类型
            System.out.println("genRightRules02 开始遍历规则： "+key);
            boolean flag1 = true;//是否是段首
            boolean flag2=false;//是否包含段尾
            ArrayList<String> value = map.get(key);
            for(String s : value) {     ////遍历每个属性的包含语句
                flag1=true;  //个人感觉应有添加 4.6晚
                flag2=false;
                String R = "";
                for (int index = 0; index < s.length(); index++) {
                    String w = s.substring(index, index + 1);
                    if (!ifcontains(endFlag, w)) {
                        R = R+w;
                    } else {
                        R = R+w;
                        break;
                    }
                }
                if(R.length()>0) {
                    flag1=false;
                    String rightRule = "";
                    if(rightRules_list.containsKey(key)){ ////判断之前已存入对应该key的规则集
                        ArrayList<String> rrule_list = rightRules_list.get(key); ////将之前存入的对应该key的规则数组取出
                        //判定循环中是否由之前的规则能符合当下规则
                        int if_endlist=0;
                        //循环遍历对应key的之前的规则集
                        for(int yi=0;yi<rrule_list.size();yi++){
                            //每次规则集的循环都重新开始迭代当前规则
                            rightRule = "";
                            String rrule = rrule_list.get(yi);
                            String[] lr = rrule.split("#");
                            boolean flag = false;//判断是否是特殊属性（父亲、儿子、女儿）
                            if (key.equals("父亲") || key.equals("儿子") || key.equals("女儿")) {
                                flag = true;
                            }
                            int index = 0;
                            for (int i = 0; i < lr.length; i++) {
                                if (index > R.length() - 1) {
                                    break;
                                }
                                String l = R.substring(index, index + 1);
                                index++;
                                if (l.equals(lr[i])) {
                                    rightRule = rightRule+"#"+l;
                                } else if (same.containsKey(key)&&(same.get(key).contains(l) && same.get(key).contains(lr[i]))) {
                                    rightRule =  rightRule+"#"+same.get(key);
                                } else if (ifcontains(endFlag, l)) {
                                    if (lr[i].contains(",")) {
                                        String[] t = lr[i].split(",");
                                        if (ifcontains(endFlag, t[0])) {
                                            if(!lr[i].contains(l)){
                                                rightRule = rightRule+"#"+l + "," + lr[i];
                                            }
                                            else {
                                                rightRule = rightRule+"#"+lr[i];
                                            }
                                        }
                                    } else {
                                        if (ifcontains(endFlag, lr[i])) {
                                            if(!lr[i].contains(l)){
                                                rightRule = rightRule+"#"+l + "," + lr[i];
                                            }
                                            else {
                                                rightRule = rightRule+"#"+lr[i];
                                            }
                                        }
                                    }
                                } else if(flag){
                                    if (l.equals("子")) {
                                        if (lr[i].equals("女")) {
                                            rightRule = rightRule+"#"+"子,女" ;
                                        } else if (lr[i].equals("子,女")) {
                                            rightRule = rightRule+"#"+"子,女" ;
                                        }
                                    }
                                    if (l.equals("女")) {
                                        if (lr[i].equals("子")) {
                                            rightRule = rightRule+"#"+"子,女" ;
                                        } else if (lr[i].equals("子,女")) {
                                            rightRule = rightRule+"#"+"子,女" ;
                                        }
                                    }
                                } else {
                                    break;
                                }
                            }
                            if(rightRule.length()>0&&rightRule.substring(0,1).equals("#")){
                                rightRule=rightRule.substring(1,rightRule.length());
                            }
                            ////右侧规则构造完成
                            // 现有想法是这里做判断：
                            //  1.如果规则与之前的第i条规则相重合，则，该规则取代原有的第i条规则入库
                            //  2.如果规则与之前的所有规则都不相符合，则，直接将最左的所有字符作为新规则入库
                            if(rightRule.length()>0){ ///(1.0)
                                rrule_list.set(yi,rightRule);
                                rightRules_list.put(key,rrule_list);
                                if_endlist=1;
                                int bef_num = rightRules_list_num.get(key).get(yi) +1;
                                rightRules_list_num.get(key).set(yi,bef_num);
                                break;
                            }
                        }
                        if(if_endlist==0){ ///(2.0)
                            if(R.length()>1){
                                for(int index3=0;index3 < R.length();index3++){
                                    rightRule = rightRule + R.substring(index3, index3 + 1) + "#";
                                }
                            }else {
                                rightRule=R;
                            }
                            if(rightRule.length()>0){
                                if(R.length()>1){
                                    rightRule = rightRule.substring(1, rightRule.length());
                                }
                                rrule_list.add(R);
                                rightRules_list.put(key,rrule_list);
                                rightRules_list_num.get(key).add(0);
                            }
                        }
                    } else {////判断之前未存入对应该key的规则集
                        if(R.length()>1){
                            for (int index3 = 0; index3 < R.length(); index3++) {
                                rightRule = rightRule + "#"+R.substring(index3, index3 + 1);
                            }
                        }
                        else {
                            rightRule=R;
                        }
                        if(rightRule.length() > 0) {
                            if(R.length()>1){
                                rightRule = rightRule.substring(1, rightRule.length());
                            }
                            ArrayList<String> r=new ArrayList<String>();
                            r.add(rightRule);
                            rightRules_list.put(key,r);
                            ArrayList<Integer> r_num = new ArrayList<Integer>();
                            r_num.add(0);
                            rightRules_list_num.put(key,r_num);
                        }
                    }
                }
                else {
                    flag2=true;
                }
            }
            if(flag1){
                if(rightRules_list.containsKey(key)){
                    ArrayList<String> rrule_list1 =rightRules_list.get(key);
                    int if_fing=0;
                    for (int i1=0;i1<rrule_list1.size();i1++){
                        if(rrule_list1.get(i1)=="$"){
                            int bef_num = rightRules_list_num.get(key).get(i1) +1;
                            rightRules_list_num.get(key).set(i1,bef_num);
                            if_fing=1;
                        }
                    }
                    if (if_fing==0){
                        rrule_list1.add("$");
                        rightRules_list.put(key,rrule_list1);
                        ArrayList<Integer> r_num= rightRules_list_num.get(key);
                        r_num.add(0);
                        rightRules_list_num.put(key,r_num);
                    }
                }
            }
            if (flag2){
                if (rightRules_list.containsKey(key)){
                    ArrayList<String> t=rightRules_list.get(key);
                    for(int i=0;i<t.size();i++){
                        t.set(i,t.get(i)+",\\n");
                    }
                    rightRules_list.put(key,t);
                }
            }
        }
        ////开始重新处理规则
        //        //按照统计原理每个key的规则取最大数的规则
        for(String key:rightRules_list.keySet()) {
            ArrayList<String> rul_list = rightRules_list.get(key);
            ArrayList<Integer> rul_num = rightRules_list_num.get(key);
            int kk = rul_num.get(0);
            int kn = 0;
            for (int i = 0; i < rul_list.size(); i++) {
                if (rul_num.get(i) > kk) {
                    kk = rul_num.get(i);
                    kn = i;
                }
            }
            //将最终结果收入最终规则集中
            String finalRule="";
            String[] per=rul_list.get(kn).split("#");
            for(int i=0;i<per.length;i++){
                if(per[i].contains(",")){
                    finalRule=finalRule+"["+per[i]+"]";
                }
                else{
                    finalRule=finalRule+per[i];
                }
            }
            System.out.println(key+"--"+finalRule);
            rightRules.put(key,finalRule);
        }
        return rightRules;
    }



    public HashMap<String,String> genRightRules(HashMap<String,ArrayList<String>> map) throws Exception {
        System.out.println("begin right rules......");
        HashMap<String,String> rightRules=new HashMap<>();
        HashMap<String,String> same=readSame();

        Set<String> set =map.keySet();
        for(String key : set) {
            System.out.println("key:"+key);
            boolean flag1=true;//是否是段尾
            boolean flag2=false;//是否包含段尾
            ArrayList<String> value = map.get(key);
            System.out.println("value:"+value+"leng="+value.size());
            for (String s : value) {
                String R = "";
                //对属性右边的内容进行初步地处理
                System.out.println("S: =" +
                        "" +
                        ""+s);
                for (int index = 0; index < s.length(); index++) {
                    String w = s.substring(index, index + 1);
                    System.out.println("w: ="+w);
                    if (!ifcontains(endFlag, w)) {
                        R = R+w;
                    } else {
                        R = R+w;
                        break;
                    }
                    System.out.println("R: ="+R);
                }

                if(R.length()>0) {
                    flag1=false;
                    String rightRule = "";
                    if (rightRules.containsKey(key)) {
                        String rrule = rightRules.get(key);
                        System.out.println("=R="+R);
                        System.out.println("-1-"+rrule+"--");
                        String[] lr = rrule.split("#");
                        System.out.println("lr_leng =  "+lr.length);
                        boolean flag = false;//判断是否是特殊属性（父亲、儿子、女儿）
                        if (key.equals("父亲") || key.equals("儿子") || key.equals("女儿")) {
                            flag = true;
                        }
                        int index = 0;
                        for (int i = 0; i < lr.length; i++) {
                            if(index>R.length()-1){
                                break;
                            }
                            String l = R.substring(index, index + 1);
                            index++;
                            System.out.println("-2-"+l+"--");
                            System.out.println("-3-"+lr[i]+"--");
                            if (l.equals(lr[i])) {
                                rightRule = rightRule+"#"+l;
                            } else if (same.containsKey(key)&&(same.get(key).contains(l) && same.get(key).contains(lr[i]))) {
                                rightRule =  rightRule+"#"+same.get(key);
                            } else if (ifcontains(endFlag, l)) {
                                if (lr[i].contains(",")) {
                                    String[] t = lr[i].split(",");
                                    if (ifcontains(endFlag, t[0])) {
                                        if(!lr[i].contains(l)){
                                            rightRule = rightRule+"#"+l + "," + lr[i];
                                        }
                                        else{
                                            rightRule = rightRule+"#"+lr[i];
                                        }
                                    }
                                } else {
                                    if (ifcontains(endFlag, lr[i])) {
                                        if(!lr[i].contains(l)){
                                            rightRule = rightRule+"#"+l + "," + lr[i];
                                        }
                                        else{
                                            rightRule = rightRule+"#"+lr[i];
                                        }
                                    }
                                }
                            } else if (flag) {
                                if (l.equals("子")) {
                                    if (lr[i].equals("女")) {
                                        rightRule = rightRule+"#"+"子,女" ;
                                    } else if (lr[i].equals("子,女")) {
                                        rightRule = rightRule+"#"+"子,女" ;
                                    }
                                }
                                if (l.equals("女")) {
                                    if (lr[i].equals("子")) {
                                        rightRule = rightRule+"#"+"子,女" ;
                                    } else if (lr[i].equals("子,女")) {
                                        rightRule = rightRule+"#"+"子,女" ;
                                    }
                                }
                            } else {
                                break;
                            }
                        }
                        if(rightRule.length()>0&&rightRule.substring(0,1).equals("#")){
                            rightRule=rightRule.substring(1,rightRule.length());
                        }
                        //右侧规则构造完成
                        rightRules.put(key, rightRule);
                        System.out.println(key+"--"+rightRule);
                    } else {
                        if(R.length()>1){
                            for (int index3 = 0; index3 < R.length(); index3++) {
                                rightRule = rightRule + "#"+R.substring(index3, index3 + 1);
                            }
                        }
                        else{
                            rightRule=R;
                        }
                        if (rightRule.length() > 0) {
                            if(R.length()>1){
                                rightRule = rightRule.substring(1, rightRule.length());
                            }
                            rightRules.put(key, rightRule);
                        }
                    }
                }
                else{
                    flag2=true;
                }
            }
            if(flag1){
                rightRules.put(key, "$");
            }
            if(flag2){
                String t=rightRules.get(key);
                t=t+",\\n";
                rightRules.put(key, t);
                System.out.println("$$$"+key+"--"+rightRules.get(key));
            }
        }
        //对规则重新整理
        for(String key:rightRules.keySet()){
            String value=rightRules.get(key);
            String[] per=value.split("#");
            String finalRule="";
            for(int i=0;i<per.length;i++){
                if(per[i].contains(",")){
                    finalRule=finalRule+"["+per[i]+"]";
                }
                else{
                    finalRule=finalRule+per[i];
                }
            }
//            System.out.println(key+"--"+finalRule+"--");
            rightRules.put(key,finalRule);
        }
        return rightRules;
    }

    //是否能添加一个循环
    public void genRules(HashMap<String,ArrayList<String>> left,HashMap<String,ArrayList<String>> right) throws Exception {
        HashMap<String,String> leftRules=genLeftRules02(left);
        HashMap<String,String> rightRules=genRightRules02(right);

        //查看数据
        System.out.println("\n\nLeftRules: "+leftRules+"\n");
        System.out.println("RightRules: "+rightRules+"\n\n");

        for(String key:leftRules.keySet()){
            if(!leftRules.get(key).equals("")) {
                if (rightRules.containsKey(key) && !rightRules.get(key).equals("")) {
                    String localRule = "";
                    if (key.equals("父亲") || key.equals("字") || key.equals("号")) {
                        localRule = leftRules.get(key) + "([\\u4E00-\\u9FA5]*?)" + rightRules.get(key);
                    } else if (key.equals("世")||key.equals("享年")) {
                        localRule = leftRules.get(key) + "([0-9 一 二 三 四 五 六 七 八 九 十 廿]*?)" + rightRules.get(key);
                    } else {
                        localRule = leftRules.get(key) + "(.*?)" + rightRules.get(key);
                    }
                    if (pro.containsKey(key)) {
                        localRules.put(pro.get(key), localRule);
                    }
                }
            }
        }
    }

    public ArrayList<String> genBeginRules(HashMap<String,ArrayList<String>> left,HashMap<String,ArrayList<String>> right) throws Exception {
        ArrayList<String> rules=new ArrayList<>();
        HashMap<String,String> leftRules=genLeftRules(left);
        HashMap<String,String> rightRules=genRightRules(right);
        for(String key:leftRules.keySet()){
            if(rightRules.containsKey(key)&&!rightRules.get(key).equals("")){
                String localRule="";
                if(key.equals("父亲")||key.equals("字")||key.equals("号")){
                    localRule=leftRules.get(key)+"([\\u4E00-\\u9FA5]*?)"+rightRules.get(key);
                }
                else if(key.equals("世")){
                    localRule=leftRules.get(key)+"([0-9 \\u4E00-\\u9FA5]*?)"+rightRules.get(key);
                }
                else{
                    localRule=leftRules.get(key)+"(.*?)"+rightRules.get(key);
                }
                rules.add(localRule);

            }
        }
        return rules;
    }

    public String[] selectBiaozhu(String filename) throws IOException {
        //读取家谱数据
        String content = ReadJiaPuFile(filename);
        //查看读取的数据文件
        System.out.print("读取数据测试："+content);
        String[] Jiapu = content.split("\n");

        //随机挑选数据
        select(Jiapu);

        return result;
    }

    //判断一段话的类型:1- 独立成段的“世”；2-一个新的人物的起始段落；3-上一个人物的描述信息
    public int validatePar(String par, ArrayList<String> rules) {
        int type;
        Pattern p = Pattern.compile(finalRules.get("shi"));
        Matcher m = p.matcher(par);
        boolean isValid = m.find();
        if (isValid) {//抽取世成功
            System.out.println("!!!"+m.group(0));
            String t = par.replace(m.group(0), "");
//            shi=m.group(1);
            if (t.length() == 0) {//独立成段的世
                type = 1;
                System.out.println("1");
            } else {
                boolean isValid1 = false;
                for(int i=0;i<rules.size();i++){
                    Pattern p2 = Pattern.compile(rules.get(i));
                    Matcher m1 = p2.matcher(par);
                    if(m1.find()){
                        isValid1=true;
                        break;
                    }
                }
                if (isValid1) {//2-一个新的人物的起始段落
                    type = 2;
                    System.out.println("2");
                } else {//3-上一个人物的描述信息
                    type = 3;
                    System.out.println("3");
                }
            }
        } else {//抽取世失败
            boolean isValid1 = false;
            for(int i=0;i<rules.size();i++){
                System.out.println("mmm"+rules.get(i));
                System.out.println(par);
                Pattern p2 = Pattern.compile(rules.get(i));
                Matcher m1 = p2.matcher(par);
                if(m1.find()){
                    isValid1=true;
                    System.out.println("mmmmmmmmmmmmmmmm");
                    break;
                }
            }
            if (isValid1) {
                type = 2;
                System.out.println("2");
            } else {
                type = 3;
                System.out.println("3");
            }
        }
        return type;
    }

    public void merge(){
        for(String key:globalRules.keySet()){
            if(localRules.containsKey(key)){
                finalRules.put(key,localRules.get(key));
            }
            else{
                finalRules.put(key,globalRules.get(key));
            }
        }
    }

    public void HAOextract(String filename) throws Exception {
        //1. 生成标注数据，供用户标注-HI
//         String[] biaozhushuju = selectBiaozhu(filename);

        /* 以下为测试所需的代码*/
        //随机挑选数据给用户进行标注（10条） 放入results
        ceshiSelect();
        //刘佳成.输出测试数据：      results是一个为10的数组
        System.out.println("测试数据输出\n"+result[3]);

        /* 以上为测试所需的代码*/


        //2. 解析标注数据      该标注数据存放在biaozhu :.txt
        parse("C:/N317/HPJiapuExtraction/src/main/knowledge/biaozhu/biaozhu-zhou.txt");

        //3. 构建局部规则库
        //生成起始规则
        ArrayList<String> beginRules=genBeginRules(beginproLeft,beginproRight);
        System.out.println("beginrules...");
        for(int i=0;i<beginRules.size();i++){
            System.out.println(beginRules.get(i));
        }

        //C:\N317\HPJiapuExtraction\src\main\knowledge
        //读取属性中文名-英文名，存储在pro中
        readProperty("C:/N317/HPJiapuExtraction/src/main/knowledge/tb_people_property.txt");

        //生成局部规则库
        System.out.println("proLeft:"+proLeft);

        //可否直接在这里添加修改语句？ 刘佳成4.4
        int prokey_i=0;
        for(String proleftkey:proLeft.keySet()){
            System.out.println(prokey_i+"proleftkey: ="+proleftkey+"    length :="+proLeft.get(proleftkey).size());
            System.out.println(proLeft.get(proleftkey));
            prokey_i+=1;
        }
        //将proleft修改为一个数组的
        //

        genRules(proLeft,proRight);
        for(String key:localRules.keySet()){
            System.out.println(key+"--"+localRules.get(key));
        }
        System.out.println("================");
        //写入局部规则
        System.out.println("写入全局规则");
        write_local_rule(localRules,"C:/N317/HPJiapuExtraction/src/main/knowledge/biaozhu/biaozhu-zhou.txt");

        System.out.println("================");

        //读取全局规则库
        readGlobalRules("C:/N317/HPJiapuExtraction/src/main/knowledge/globalRules.txt");

        //合并局部规则库和全局规则库
        merge();
        for(String key:finalRules.keySet()){
            System.out.println(key+"--"+finalRules.get(key));
        }

        //4. 信息抽取
        //读取家谱数据
        String content = ReadJiaPuFile(filename);
        String[] Jiapu = content.split("\n");

        //抽取
        ArrayList<String> nh = readNianhao();
        ArrayList<HashMap<String,String>> p=new ArrayList<>();
        int personNum = 0;
        String shi = "";
        String data="";//当前待处理的家谱数据
        for (int i = 0; i < Jiapu.length; i++) {
            if (Jiapu[i] != "") {
                //判断该段话的类型：
                if(validatePar(Jiapu[i],beginRules)==1){//1. 当前段落为独立成段的世信息
                    //世
                    String pattern = finalRules.get("shi");
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(Jiapu[i]);
                    if (m.find()) {
                        System.out.println("shi:"+m.group(1));
                        shi = m.group(1).trim();
                        shi=shi.replace(" ","");
                        if(shi.contains("第")){
                            shi=shi.replace("第","");
                        }
                    }
                }
                else if(validatePar(Jiapu[i],beginRules)==2){
                    data=Jiapu[i];
                    if(i+1< Jiapu.length){
                        for(int j=i+1;j< Jiapu.length;j++){
                            if(validatePar(Jiapu[j],beginRules)==3){
                                data=data+Jiapu[j];
                            }
                            else{
                                i=j-1;
                                break;
                            }
                        }
                    }
                    //抽取信息
                    //世
                    String pattern = finalRules.get("shi");
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(data);
                    if (m.find()) {
                        System.out.println(m.group(1));
                        shi = m.group(1).trim();
                        shi=shi.replace(" ","");
                        if(shi.contains("第")){
                            shi=shi.replace("第","");
                        }
                    }
                    //姓名
                    Pattern rname = Pattern.compile(finalRules.get("name"));
                    Matcher mname = rname.matcher(data);
                    if(mname.find()) {
                        String name = mname.group(1).trim();
                            //对抽取后的姓名进行进一步处理
                            //1. 去除姓名中的“公”
                            if (name.length() > 1) {
                                if (name.substring(name.length() - 1).equals("公")) {
                                    name = name.replaceAll("公", "");
                                }
                            }
                            //2. 去除姓名中的世
                            if (name.contains("第" + shi + "世")) {
                                name = name.replace("第" + shi + "世", "").trim();
                            } else if (name.contains(shi + "世")) {
                                name = name.replace(shi + "世", "").trim();
                            }
                            //3. 去除姓名中的空格
                        name=name.replace("　","");
                        if(!name.equals("")) {
                            HashMap<String, String> people = new HashMap<>();
                            people.put("id", String.valueOf(personNum));
                            people.put("shi", shi);
                            System.out.println("name:"+name);
                            people.put("name", name);
                            String sex = "";
                            //各属性及关系抽取
                            for (String key : finalRules.keySet()) {
                                if (!key.equals("shi") && !key.equals("name")) {
                                    if (key.equals("father_id")) {//父亲
                                        Pattern rfathername = Pattern.compile(finalRules.get("father_id"));
                                        Matcher mfathername = rfathername.matcher(data);
                                        if (mfathername.find()) {
                                            System.out.println("父亲姓名" + mfathername.group(1));
                                            String rankRegex = mfathername.group(1) + "(.*?子)";
                                            Pattern rank = Pattern.compile(rankRegex);
                                            Matcher mrank = rank.matcher(data);
                                            if (mrank.find()) {
                                                System.out.println(mrank.group(1));
                                                int fatherrank = getFamilyRank(mrank.group(1));
                                                people.put("rank_family", String.valueOf(fatherrank));
                                                people.put("sex", "男");
                                                sex = "男";
                                            } else {
                                                String rrankRegex = mfathername.group(1) + "(.*?女)";
                                                Pattern rrank = Pattern.compile(rrankRegex);
                                                Matcher mrrank = rrank.matcher(data);
                                                if (mrrank.find()) {
                                                    System.out.println(mrrank.group(1));
                                                    int fatherrank = getFamilyRank(mrrank.group(1));
                                                    people.put("rank_family", String.valueOf(fatherrank));
                                                    people.put("sex", "女");
                                                    sex = "女";
                                                }
                                            }
                                            String fa = mfathername.group(1);
                                            if (!fa.equals("")) {
                                                if (fa.substring(fa.length() - 1).equals("公")) {
                                                    fa = fa.replace("公", "");
                                                    fa = solveFa(fa);
                                                    people.put("father_name", fa);
                                                    System.out.println("aaaaaaa爸爸" + fa);
                                                } else {
                                                    people.put("father_name", fa);
                                                }
                                            }
                                        } else {
                                            people.put("sex", "男");
                                        }
                                    } else if (key.equals("birth")) {
                                        Pattern rbirth = Pattern.compile(finalRules.get("birth"));
                                        Matcher mbirth = rbirth.matcher(data);
                                        if (mbirth.find()) {
                                            System.out.println("出生日期" + mbirth.group(1));
                                            boolean flag1 = false;
                                            for (int na = 0; na < nh.size(); na++) {
                                                String temp = nh.get(na);
                                                if (mbirth.group(1).contains(temp)) {
                                                    people.put("china_birth", mbirth.group(1));
                                                    flag1 = true;
                                                    break;
                                                }
                                            }
                                            if (!flag1) {
                                                people.put("birth", mbirth.group(1));
                                            }
                                        }
                                    } else if (key.equals("death_date")) {
                                        Pattern rdeath = Pattern.compile(finalRules.get("death_date"));
                                        Matcher mdeath = rdeath.matcher(data);
                                        if (mdeath.find()) {
                                            System.out.println("过世日期" + mdeath.group(1));
                                            boolean flag2 = false;
                                            for (int na = 0; na < nh.size(); na++) {
                                                String temp = nh.get(na);
                                                if (mdeath.group(1).contains(temp)) {
                                                    people.put("china_death", mdeath.group(1));
                                                    flag2 = true;
                                                    break;
                                                }
                                            }
                                            if (!flag2) {
                                                people.put("death_date", mdeath.group(1));
                                            }
                                        }
                                    } else if (key.equals("son_id")) {
                                        Pattern rson = Pattern.compile(finalRules.get("son_id"));
                                        Matcher mson = rson.matcher(data);
                                        if (mson.find()) {
                                            System.out.println("儿子" + mson.group(1));
                                            if (finalRules.containsKey("son_name")) {
                                                Pattern son_p = Pattern.compile(finalRules.get("son_id"));
                                                Matcher son_m = son_p.matcher(mson.group(1));
                                                String sonname = "";
                                                while (son_m.find()) {
                                                    sonname = sonname + "、" + son_m.group(1);
                                                }
                                                sonname.substring(1, sonname.length());
                                                people.put("son_id", sonname);
                                            } else {
                                                people.put("son_id", mson.group(1));
                                            }
                                        }
                                    } else if (key.equals("daughter_id")) {
                                        Pattern rson = Pattern.compile(finalRules.get("daughter_id"));
                                        Matcher mson = rson.matcher(data);
                                        if (mson.find()) {
                                            System.out.println("女儿" + mson.group(1));
                                            if (finalRules.containsKey("son_name")) {
                                                Pattern son_p = Pattern.compile(finalRules.get("daughter_id"));
                                                Matcher son_m = son_p.matcher(mson.group(1));
                                                String sonname = "";
                                                while (son_m.find()) {
                                                    sonname = sonname + "、" + son_m.group(1);
                                                }
                                                sonname.substring(1, sonname.length());
                                                people.put("daughter_id", sonname);
                                            } else {
                                                people.put("daughter_id", mson.group(1));
                                            }
                                        }
                                    } else if (key.equals("partner_id")) {
                                        int temp = personNum;
//                                        System.out.println(personNum);
                                        //配偶姓名
                                        Pattern rpartnername = Pattern.compile(finalRules.get("partner_id"));
                                        Matcher mpartnername = rpartnername.matcher(data);
                                        if (mpartnername.find()&&!mpartnername.group(1).equals("")) {
                                            System.out.println("配偶姓名" + mpartnername.group(1));
                                            String[] partner = mpartnername.group(1).split("、");
                                            for (int a = 0; a < partner.length; a++) {
                                                if(!partner[a].equals("")) {
                                                    personNum = personNum + 1;
                                                    HashMap<String, String> newp = new HashMap<>();
                                                    newp.put("id", String.valueOf(personNum));
                                                    newp.put("name", partner[a]);
                                                    if (sex.equals("女")) {
                                                        newp.put("sex", "男");
                                                    } else {
                                                        newp.put("sex", "女");
                                                    }
                                                    newp.put("partner_id", String.valueOf(temp));
                                                    p.add(newp);
                                                }
                                            }
                                            String par = "";
//                                            System.out.println(temp+1);
//                                            System.out.println(personNum);
                                            for (int b = temp + 1; b <= personNum; b++) {
                                                if (b < personNum)
                                                    par = par + String.valueOf(b) + "/";
                                                else
                                                    par = par + String.valueOf(b);
                                            }
                                            people.put("partner_id", par);
                                        }
                                    } else {
                                        Pattern pp = Pattern.compile(finalRules.get(key));
                                        Matcher mm = pp.matcher(data);
                                        if (mm.find()) {
                                            people.put(key, mm.group(1));
                                        }
                                    }
                                }
                            }
                            personNum++;
                            p.add(people);
                        }
                    }
                }
            }
        }
//        System.out.println(personNum);
        //2 循环为家谱人物建立父子关系
        //2.1姓名完整的情况下
        //按人物父亲姓名构建父子关系
        for (int i = 0; i < p.size(); i++) {
            HashMap<String,String> per=p.get(i);
            if (per.containsKey("father_name")) {
//                System.out.println("name"+per.get("name"));
//                System.out.println("fathername"+per.get("father_name"));
                for (int y = i; y >= 0; y--) {
                    HashMap<String,String> pper=p.get(y);
//                    if (per.get("father_name").equals(pper.get("name"))) {
                    if (pper.get("sex").equals("男")&&pper.get("name").contains(per.get("father_name"))) {
//                        System.out.println("2name"+pper.get("name"));
//                        System.out.println("2id"+pper.get("id"));
                        per.put("father_id",pper.get("id"));
                        if(pper.containsKey("partner_id")&&!pper.get("partner_id").contains("/")){
                            per.put("mother_id",pper.get("partner_id"));
                        }
                        p.set(i,per);
                        break;
                    }
                }
            }
        }
        //按人物儿子姓名构建父子关系
        for (int i = 0; i < p.size(); i++) {
            HashMap<String,String> per=p.get(i);
            if (per.containsKey("son_id")) {
                String[] son;
                if(!getSplit(per.get("son_id")).equals("")){
                    son=per.get("son_id").split(getSplit(per.get("son_id")));
                }
                else{
                    son=per.get("son_id").split("、");
                }
                for (int x = 0; x < son.length; x++) {
                    for (int j = i; j < personNum; j++) {
                        HashMap<String,String> pper=p.get(j);
                        if (pper.get("name").equals(son[x].trim())) {
                            if(!pper.containsKey("father_id")){
                                pper.put("father_id",per.get("id"));
                                if ((per.containsKey("partner_id")) && (!per.get("partner_id").contains("/"))){
                                    pper.put("mother_id",per.get("partner_id"));
                                }
                                p.set(j,pper);
                            }
                            break;
                        }
                    }
                }
            }
        }

        //按人物nver姓名构建父子关系
        for (int i = 0; i < personNum; i++) {
            HashMap<String,String> per=p.get(i);
            if (per.containsKey("daughter_id")) {
//                String[] dau=per.get("daughter_id").split("、");
                String[] dau;
                if(!getSplit(per.get("daughter_id")).equals("")){
                    dau=per.get("daughter_id").split(getSplit(per.get("daughter_id")));
                }
                else{
                    dau=per.get("daughter_id").split("、");
                }
                for (int x = 0; x < dau.length; x++) {
                    for (int j = i; j < personNum; j++) {
                        HashMap<String,String> pper=p.get(j);
                        if (pper.get("name").equals(dau[x])) {
                            if(!pper.containsKey("father_id")) {
                                pper.put("father_id", per.get("id"));
                                if ((per.containsKey("partner_id")) && (!per.get("partner_id").contains("/"))) {
                                    pper.put("mother_id", per.get("partner_id"));
                                }
                                p.set(j, pper);
                            }
                            break;
                        }
                    }
                }
            }
        }

        //姓名不完整的情况下
        //按人物儿子姓名构建父子关系
//        for (int i = 0; i < personNum; i++) {
//            son = p[i].sonName;
//            if (son != null) {
//                for (int x = 0; x < son.length; x++) {
//                    if (son[x].trim().length() == 1) {
//
//                        String fa = p[i].name.substring(0, 1);
//                        int r = getIndex(rank_family, fa);
//                        if (r != 0) {
//                            String s = rank_family[r + 1];
//                            String sn = s + son[x];
//                            System.out.println(son[x]);
//                            System.out.println(sn);
//                            for (int j = i; j < personNum; j++) {
//                                if (p[j].name.length() > 1) {
//                                    if (p[j].name.equals(sn) && p[j].fatherid == -1) {
//                                        p[j].fatherid = p[i].id;
//                                        if ((p[i].partnerid != "") && (!p[i].partnerid.contains("/")))
//                                            p[j].motherid = Integer.valueOf(p[i].partnerid);
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
        //按人物父亲姓名构建父子关系
        //1. 成公，XX之子
        //XX，成公之子
        for (int i = 0; i < personNum; i++) {
            HashMap<String,String> per=p.get(i);
            if (per.containsKey("father_name") && !per.containsKey("father_id")) {
                for (int y = i; y >= 0; y--) {
                    HashMap<String,String> pper=p.get(y);
                    if (pper.get("name").length() > 1) {
                        if (pper.get("name").substring(pper.get("name").length() - 1).equals(per.get("father_name"))) {
                            if(!per.containsKey("father_id")) {
                                per.put("father_id", pper.get("id"));
                                if ((pper.containsKey("partner_id")) && (!pper.get("partner_id").contains("/"))) {
                                    per.put("mother_id", pper.get("partner_id"));
                                }
                                p.set(i, per);
                            }
                            break;
                        }
                    }
                }
            }
        }
        //2. 黄成成，XX之子
        //XX，成成之子
        for (int i = 0; i < personNum; i++) {
            HashMap<String,String> per=p.get(i);
            if (per.containsKey("father_name") && !per.containsKey("father_id")) {
//                System.out.println("ccc"+per.get("name"));
                for (int y = i; y >= 0; y--) {
                    HashMap<String,String> pper=p.get(y);
                    if (pper.get("name").length() > 1) {
//                        System.out.println("aaa"+per.get("father_name"));
//                        System.out.println("bbb"+pper.get("name").substring(1,pper.get("name").length()));
                        if (pper.get("name").substring(1,pper.get("name").length()).equals(per.get("father_name"))) {
                            if(!per.containsKey("father_id")) {
                                per.put("father_id", pper.get("id"));
                                if ((pper.containsKey("partner_id")) && (!pper.get("partner_id").contains("/"))) {
                                    per.put("mother_id", pper.get("partner_id"));
                                }
                                p.set(i, per);
                            }
                            break;
                        }
                    }
                }
            }
        }
        //数据提取的路径
        String dirPath = "E:/jiaPuData/";
        File file = new File(dirPath);
        System.out.println("\n\np:"+p);
        if (file.exists()) {
            System.out.println("文件夹存在");
        } else {
            System.out.println("文件夹不存在，创建一个新的");
            file.mkdir();
        }
        //存储为excel文件
        String outFileName = dirPath + "zhou(4.7).xls";

        addExcel(outFileName, p);


    }

    public String getSplit(String s){
        List<String> list = new ArrayList<>();
        list.add("、");
        list.add("；");
        list.add(" ");
        HashMap<String,Integer> charCount=new HashMap<>();
        int max=0;
        String result ="";
        for(int i=0;i<s.length();i++){
            String c=s.substring(i,i+1);
            if(list.contains(c)){
                if(charCount.containsKey(c)){
                    int count=charCount.get(c);
                    count++;
                    if(count>max){
                        max=count;
                        result=c;
                    }
                    charCount.put(c,count);
                }
                else{
                    charCount.put(c,1);
                }
            }
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        wordJiapuExtraction ex = new wordJiapuExtraction();
        //循环读取文件中的数据           这里读取的是原文件

//        File file=new File("C:\\N317\\HPJiapuExtraction\\src\\main\\knowledge\\file_ways.txt");
//        BufferedReader reader=null;
//        try {
//            reader = new BufferedReader(new FileReader(file));
//            String tempString = null;
//            int line = 1;
//            while ((tempString = reader.readLine()) != null) {
//                //进入信息抽取
//                ex.HAOextract(tempString);
//
//            }
//            System.out.println("end");
//        }catch (IOException e){
//            e.printStackTrace();
//        }finally {
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException e1) {
//                }
//            }
//        }

        ex.HAOextract("C:\\N317\\数据标注完成\\汤坝周氏族谱--3000780\\源文件.docx");
        //ex.selectBiaozhu("C:\\N317\\HPJiapuExtraction\\src\\main\\\\knowledge\\biaozhu2\\0.txt");
//        System.out.println(ex.getSplit("hhh"));

    }

}
