package me.gv7.tools.josearcher.searcher;

import me.gv7.tools.josearcher.entity.Blacklist;
import me.gv7.tools.josearcher.entity.Keyword;
import me.gv7.tools.josearcher.entity.NodeT;
import me.gv7.tools.josearcher.utils.CheckUtil;
import me.gv7.tools.josearcher.utils.CommonUtil;
import me.gv7.tools.josearcher.utils.LogUtil;
import me.gv7.tools.josearcher.utils.MatchUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import static me.gv7.tools.josearcher.utils.CommonUtil.*;
import static me.gv7.tools.josearcher.utils.CommonUtil.getBlank;

public class SearchObjectByBFS {
    private String model_name = SearchObjectByBFS.class.getSimpleName();
    private Object target;
    private List<Keyword> keys = new ArrayList<>();
    private List<Blacklist> blacklists = new ArrayList<>();
    private int max_search_depth = Integer.MAX_VALUE;/* 递归搜索深度 */
    private boolean is_debug = true;
    private int number = 0;
    //记录所有访问过的元素
    private Set<Object> visited = new HashSet<Object>();
    //用队列存放所有依次要访问元素
    private Queue<NodeT> q = new LinkedList<NodeT>();
    //把当前的元素加入到队列尾
    private String report_save_path = null;
    private String result_file;
    private String all_chain_file;
    private String err_log_file;
    private String line = "==============================================";

    public SearchObjectByBFS(Object target, String objectStatement , List<Keyword> keys){
        this.target = target;
        this.keys = keys;

        String initStatement = String.format("%s = %s;\nClass aClass = null;\nField declaredField = null;\nObject[] objectArray = null;\nMethod declaredMethod = null;\nint i = 0;\nList list = null;\nSet set = null;\nMap map = null;\nIterator iterator = null;\nIterator<Map.Entry<Object, Object>> it = null;\nMap.Entry<Object, Object> entry = null;\n\n","Object object",objectStatement);

        //把当前的元素加入到队列尾
        q.offer(new NodeT.Builder().setChain("").setField_name("TargetObject").setField_object(target).setCurrent_depth(0).setStatement(initStatement).build());
    }

    public void initSavePath(){
        if(report_save_path == null){
            this.result_file = String.format("%s_result_%s.txt",model_name,getCurrentDate());
            this.all_chain_file = String.format("%s_log_%s.txt",model_name,getCurrentDate());
            this.err_log_file = String.format("%s_error_%s.txt",model_name,getCurrentDate());
        }else{
            this.result_file = String.format("%s/%s_result_%s.txt",report_save_path,model_name,getCurrentDate());
            this.all_chain_file = String.format("%s/%s_log_%s.txt",report_save_path,model_name,getCurrentDate());
            this.err_log_file = String.format("%s_error_%s.txt",report_save_path,model_name,getCurrentDate());
        }
    }

    public void setBlacklists(List<Blacklist> blacklists) {
        this.blacklists = blacklists;
    }

    public void setMax_search_depth(int max_search_depth) {
        this.max_search_depth = max_search_depth;
    }

    public void setReport_save_path(String report_save_path) {
        this.report_save_path = report_save_path;
    }

    public void setIs_debug(boolean is_debug) {
        this.is_debug = is_debug;
    }

    public void setErrLogFile(String err_log_file) {
        this.err_log_file = err_log_file;
    }

    public void searchObject(){
        this.initSavePath();
        while(!q.isEmpty()){
            NodeT node = q.poll();
            String filed_name = node.getField_name();
            Object filed_object = node.getField_object();
            String log_chain = node.getChain();
            String new_log_chain = null;
            String preStatement = node.getStatement();
            int current_depth = node.getCurrent_depth();

            //最多挖多深
            if(current_depth > max_search_depth){
                continue;
            }

            if (filed_object == null || CheckUtil.isSysType(filed_object) || MatchUtil.isInBlacklist(filed_name,filed_object,this.blacklists)){
                //如果object是null/基本数据类型/包装类/日期类型，则不需要在递归调用
                continue;
            }

            try {
                //被访问过了，就不访问，防止死循环
                // 注意：Set.contains 可能存在空指针异常
                if (!visited.contains(filed_object)) {
                    visited.add(filed_object);

                    if (log_chain != null && log_chain != "") {
                        new_log_chain = String.format("%s \n%s ---> %s = {%s}", log_chain, getBlank(current_depth + 1), filed_name, filed_object.getClass().getName());
                    } else {
                        new_log_chain = String.format("%s = {%s}", "TargetObject", filed_object.getClass().getName());
                    }

                    // 搜索操作
                    if (MatchUtil.matchObject(filed_name, filed_object, keys)) {
                        number ++;
                        write2log(result_file, "chain"+number+":\n\n" + new_log_chain + "\n\n\n","codes"+number+":\n\n" + line + "\n\n"+preStatement+"\n\n" +line +"\n\n\n\n");
                    }
                    if (is_debug) {
                        write2log(all_chain_file, new_log_chain + "\n\n\n");
                    }
                    System.out.println(new_log_chain);

                    // 对属性为set、map、list类型进行处理
                    // 将set、map、list中的元素取出并加入队列，不增加层数
                    if (filed_object instanceof List) {

                        List list = (List) filed_object;
                        if(list != null && list.size() > 0) {

                            for (int i = 0;i < list.size();i ++) {
                                String statement = preStatement + CommonUtil.generateCode(1, i);
                                NodeT n = new NodeT.Builder().setField_name(String.valueOf(i)).setField_object(list.get(i)).setChain(new_log_chain).setCurrent_depth(current_depth).setStatement(statement).build();
                                q.offer(n);
                            }
                        }
                        continue;
                    }

                    if (filed_object instanceof Map) {

                        Map map = (Map) filed_object;
                        if (map != null && map.size() > 0) {

                            Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
                            int i = 0;
                            while (it.hasNext()) {
                                String statement = preStatement + CommonUtil.generateCode(3, i);
                                Map.Entry<Object, Object> entry = it.next();
                                NodeT n = new NodeT.Builder().setField_name(String.valueOf(entry.getKey())).setField_object(entry.getValue()).setChain(new_log_chain).setCurrent_depth(current_depth).setStatement(statement).build();
                                q.offer(n);
                                i ++;
                            }
                        }

                        continue;
                    }

                    if(filed_object instanceof Set){

                        Set set = (Set) filed_object;
                        if (set != null && set.size() > 0){
                            int i = 0;
                            Iterator iterator = set.iterator();
                            while (iterator.hasNext()) {
                                String statement = preStatement + CommonUtil.generateCode(2, i);
                                NodeT n = new NodeT.Builder().setField_name(String.valueOf(i)).setField_object(iterator.next()).setChain(new_log_chain).setCurrent_depth(current_depth).setStatement(statement).build();
                                q.offer(n);
                                i ++;
                            }
                        }

                        continue;
                    }

                    Class clazz = filed_object.getClass();

                    for (int index = 0; clazz != Object.class; clazz = clazz.getSuperclass(), index ++) {//向上循环 遍历父类
                        Field[] fields = clazz.getDeclaredFields();

                        for (Field field : fields) {
                            field.setAccessible(true);
                            String proType = field.getGenericType().toString();
                            String proName = field.getName();
                            Object subObj = null;
                            try {
                                subObj = field.get(filed_object);
                            } catch (Throwable e) {
                                LogUtil.saveThrowableInfo(e, this.err_log_file);
                                continue;
                            }

                            if (subObj == null) {
                                continue;
                            } else if (CheckUtil.isSysType(field)) {
                                //属性是系统类型跳过
                                continue;
                            } else if (subObj.getClass().getName().indexOf("_") != -1){
                                //判断类中是否包含一些特殊的符号，例如"_"等
                                continue;
                            } else if (MatchUtil.isInBlacklist(proName, subObj, this.blacklists)) {
                                continue;
                            } else if (field.getType().isArray()) {
                                try {
                                    //属性的类型为数组
                                    Object obj = field.get(filed_object);
                                    if (obj == null) {
                                        continue;
                                    }

                                    Object[] objArr = (Object[]) obj;
                                    if (objArr != null && objArr.length > 0) {

                                        for (int i = 0; i < objArr.length; i++) {
                                            if (objArr[i] == null) {
                                                continue;
                                            }

//                                            特意添加获取真实的thread名的代码，这里不能使用下标获取，因为带有随机性
                                            String statement = "";
                                            if(proName == "threads") {
                                                Thread thread = (Thread) objArr[i];
                                                String threadName = thread.getName();
                                                statement = preStatement + CommonUtil.generateCode(index,proName,2, i , threadName);
                                            } else {
                                                statement = preStatement + CommonUtil.generateCode(index,proName,2, i);
                                            }

                                            String arr_name = String.format("%s[%d]", proName, i);
                                            NodeT n = new NodeT.Builder().setField_name(arr_name).setField_object(objArr[i]).setChain(new_log_chain).setCurrent_depth(current_depth + 1).setStatement(statement).build();
                                            q.offer(n);
                                        }
                                    }
                                } catch (Throwable e) {
                                    LogUtil.saveThrowableInfo(e, this.err_log_file);
                                }
                            } else {
                                try {
                                    //属性的类型为普通类型
                                    String statement = preStatement + CommonUtil.generateCode(index,proName,1);

                                    NodeT n = new NodeT.Builder().setField_name(proName).setField_object(subObj).setChain(new_log_chain).setCurrent_depth(current_depth + 1).setStatement(statement).build();
                                    q.offer(n);
                                } catch (Throwable e) {
                                    //logger.error(String.format("%s - %s",model_name,"class"),e);
                                    LogUtil.saveThrowableInfo(e, this.err_log_file);
                                }
                            }
                        }
                    }
                }
            }catch (Throwable e){
                LogUtil.saveThrowableInfo(e,this.err_log_file);
            }
        }
    }
}
