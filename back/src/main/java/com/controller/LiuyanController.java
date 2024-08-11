
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 留言板
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/liuyan")
public class LiuyanController {
    private static final Logger logger = LoggerFactory.getLogger(LiuyanController.class);

    private static final String TABLE_NAME = "liuyan";

    @Autowired
    private LiuyanService liuyanService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private CaozuorizhiService caozuorizhiService;//操作日志
    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private NewsService newsService;//公告信息
    @Autowired
    private ShiwuzhaolingService shiwuzhaolingService;//失物信息
    @Autowired
    private ShiwuzhaolingYuyueService shiwuzhaolingYuyueService;//失物认领
    @Autowired
    private XunwuqishiService xunwuqishiService;//寻物启事
    @Autowired
    private XunwuqishiLiuyanService xunwuqishiLiuyanService;//寻物启事留言
    @Autowired
    private YonghuService yonghuService;//用户
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        CommonUtil.checkMap(params);
        PageUtils page = liuyanService.queryPage(params);

        //字典表数据转换
        List<LiuyanView> list =(List<LiuyanView>)page.getList();
        for(LiuyanView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"列表查询",list.toString());
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        LiuyanEntity liuyan = liuyanService.selectById(id);
        if(liuyan !=null){
            //entity转view
            LiuyanView view = new LiuyanView();
            BeanUtils.copyProperties( liuyan , view );//把实体数据重构到view中
            //级联表 用户
            //级联表
            YonghuEntity yonghu = yonghuService.selectById(liuyan.getYonghuId());
            if(yonghu != null){
            BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setYonghuId(yonghu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
    caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"单条数据查看",view.toString());
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody LiuyanEntity liuyan, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,liuyan:{}",this.getClass().getName(),liuyan.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("用户".equals(role))
            liuyan.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<LiuyanEntity> queryWrapper = new EntityWrapper<LiuyanEntity>()
            .eq("yonghu_id", liuyan.getYonghuId())
            .eq("liuyan_name", liuyan.getLiuyanName())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        LiuyanEntity liuyanEntity = liuyanService.selectOne(queryWrapper);
        if(liuyanEntity==null){
            liuyan.setInsertTime(new Date());
            liuyan.setCreateTime(new Date());
            liuyanService.insert(liuyan);
            caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"新增",liuyan.toString());
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody LiuyanEntity liuyan, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,liuyan:{}",this.getClass().getName(),liuyan.toString());
        LiuyanEntity oldLiuyanEntity = liuyanService.selectById(liuyan.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("用户".equals(role))
//            liuyan.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        liuyan.setUpdateTime(new Date());

            liuyanService.updateById(liuyan);//根据id更新
            List<String> strings = caozuorizhiService.clazzDiff(liuyan, oldLiuyanEntity, request,new String[]{"updateTime"});
            caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"修改",strings.toString());
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<LiuyanEntity> oldLiuyanList =liuyanService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        liuyanService.deleteBatchIds(Arrays.asList(ids));

        caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"删除",oldLiuyanList.toString());
        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //.eq("time", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
        try {
            List<LiuyanEntity> liuyanList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            LiuyanEntity liuyanEntity = new LiuyanEntity();
//                            liuyanEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            liuyanEntity.setLiuyanName(data.get(0));                    //留言标题 要改的
//                            liuyanEntity.setLiuyanText(data.get(0));                    //留言内容 要改的
//                            liuyanEntity.setInsertTime(date);//时间
//                            liuyanEntity.setReplyText(data.get(0));                    //回复内容 要改的
//                            liuyanEntity.setUpdateTime(sdf.parse(data.get(0)));          //回复时间 要改的
//                            liuyanEntity.setCreateTime(date);//时间
                            liuyanList.add(liuyanEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        liuyanService.insertBatch(liuyanList);
                        caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"批量新增",liuyanList.toString());
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = liuyanService.queryPage(params);

        //字典表数据转换
        List<LiuyanView> list =(List<LiuyanView>)page.getList();
        for(LiuyanView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"列表查询",list.toString());
        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        LiuyanEntity liuyan = liuyanService.selectById(id);
            if(liuyan !=null){


                //entity转view
                LiuyanView view = new LiuyanView();
                BeanUtils.copyProperties( liuyan , view );//把实体数据重构到view中

                //级联表
                    YonghuEntity yonghu = yonghuService.selectById(liuyan.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                    caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"单条数据查看",view.toString());
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody LiuyanEntity liuyan, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,liuyan:{}",this.getClass().getName(),liuyan.toString());
        Wrapper<LiuyanEntity> queryWrapper = new EntityWrapper<LiuyanEntity>()
            .eq("yonghu_id", liuyan.getYonghuId())
            .eq("liuyan_name", liuyan.getLiuyanName())
            .eq("liuyan_text", liuyan.getLiuyanText())
            .eq("reply_text", liuyan.getReplyText())
//            .notIn("liuyan_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        LiuyanEntity liuyanEntity = liuyanService.selectOne(queryWrapper);
        if(liuyanEntity==null){
            liuyan.setInsertTime(new Date());
            liuyan.setCreateTime(new Date());
        liuyanService.insert(liuyan);

            caozuorizhiService.insertCaozuorizhi(String.valueOf(request.getSession().getAttribute("role")),TABLE_NAME,String.valueOf(request.getSession().getAttribute("username")),"前台新增",liuyan.toString());
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

}

