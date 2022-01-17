package me.gv7.tools.josearcher.utils;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommonUtil {
    public static String getBanner(){
        String banner = "#############################################################\n" +
                        "   Java Object Searcher v0.01\n" +
                        "   change_by: doubleq original_author: c0ny1<root@gv7.me>\n" +
                        "   github: https://github.com/likCodinG/java-object-searcher\n"+
                        "#############################################################\n\n\n";
        return banner;
    }

    public static void write2log(String filename,String content){
        try {
            File file = new File(filename);
            String new_content;
            if (!file.exists()) {
                file.createNewFile();
                new_content = getBanner() + content;
            }else{
                new_content = content;
            }

            //使用true，即进行append file
            FileWriter fileWritter = new FileWriter(file, true);
            fileWritter.write(new_content);
            fileWritter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void write2log(String filename,String content,String codes){
        try {
            File file = new File(filename);
            String new_content;
            if (!file.exists()) {
                file.createNewFile();
                new_content = getBanner() + content + codes;
            }else{
                new_content = content + codes;
            }

            //使用true，即进行append file
            FileWriter fileWritter = new FileWriter(file, true);
            fileWritter.write(new_content);
            fileWritter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getBlank(int n){
        String strTab = "";
        for(int i=0;i<n;i++){
            strTab += " ";
        }
        return strTab;
    }

    public static String getCurrentDate(){
        Date date = new Date();
        String str = "yyyMMddHHmmss";
        SimpleDateFormat sdf = new SimpleDateFormat(str);
        return sdf.format(date);
    }


    /**
     * 对普通类型以及Array类型进行代码生成，首先遍历获取到对应的filed
     * @param index
     * @param proName
     * @param condition
     * @param args
     * @return
     */
    public static String generateCode(int index, String proName, int condition,Object...args) {

        String statement = "";
        String tmp = null;

//        获取对应父类的class对象
        if(index > 0){
            tmp = String.format(
                    "aClass = object.getClass();\n" +
                    "for (int j = 1;j <= %d;j ++) aClass = aClass.getSuperclass();\n",index);
        } else {
            tmp = String.format("aClass = object.getClass();\n");
        }

        statement += tmp;

        tmp = String.format(
                "declaredField = aClass.getDeclaredField(\"%s\");\n" +
                "declaredField.setAccessible(true);\n" +
                "object = declaredField.get(object);\n\n",proName);
        statement += tmp;

//        处理array的情况
        if (condition == 2) {
            if (args.length == 1) {
                tmp = String.format("objectArray = (Object[]) object;\nobject = objectArray[%s];\n\n", args[0]);
                statement += tmp;
            } else {
                tmp = String.format(
                        "objectArray = (Object[]) object;\n" +
                        "for (Object obj: objectArray) {\n" +
                        "    if(obj == null) continue;\n" +
                        "    Thread obj1 = (Thread) obj;\n" +
                        "    if(obj1.getName().indexOf(\"%s\") != -1) {\n" +
                        "        object = obj;\n" +
                        "        break;\n" +
                        "    }\n" +
                        "}\n",args[1]);
                statement += tmp;
            }
        }

        return statement;
    }

    /**
     * 生成Set、Map、List类型的代码
     * condition对应不同的类型
     * @param condition
     * @param index
     * @return
     */
    public static String generateCode(int condition, int index) {

        String statement = "";

        switch (condition) {
            case 1:statement = String.format("list = (List) object;\nobject = list.get(%d);\n\n",index); break;
            case 2:statement = String.format(
                            "set = (Set) object;\n" +
                            "iterator = set.iterator();\n" +
                            "i = 0;\n" +
                            "while (iterator.hasNext()) {\n" +
                            "    if(%d == i) {\n" +
                            "        object = iterator.next();\n" +
                            "        break;\n" +
                            "    }\n" +
                            "    i ++;\n" +
                            "}\n\n",index);break;
            case 3: statement = String.format(
                            "map = (Map) object;\n" +
                            "it = map.entrySet().iterator();\n" +
                            "i = 0;\n" +
                            "while (it.hasNext()) {\n" +
                            "    entry = it.next();\n" +
                            "    if(i == %d) {\n" +
                            "        object = entry.getValue();\n" +
                            "        break;\n" +
                            "    }\n" +
                            "    i ++;\n" +
                            "}\n\n",index);break;
            default:break;
        }
        return statement;
    }
}
