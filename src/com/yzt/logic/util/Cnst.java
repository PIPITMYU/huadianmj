package com.yzt.logic.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.yzt.logic.mj.dao.UserMapper;


/**
 * 常量
 */
public class Cnst {
	
	public final static String PING_MESSAGE = "ping";
	
	// 获取项目版本信息
    public static final String version = ProjectInfoPropertyUtil.getProperty("project_version", "1.5");
    public static Boolean isTest = true;//是否是测试环境


    public static final String p_name = ProjectInfoPropertyUtil.getProperty("p_name", "wsw_X17");
    public static final String o_name = ProjectInfoPropertyUtil.getProperty("o_name", "u_consume");
    public static final String gm_url = ProjectInfoPropertyUtil.getProperty("gm_url", "");
    
    //回放配置
    public static final String BACK_FILE_PATH = ProjectInfoPropertyUtil.getProperty("backFilePath", "1.5");
    public static final String FILE_ROOT_PATH = ProjectInfoPropertyUtil.getProperty("fileRootPath", "1.5");
    public static final String INTERFACED_FILE_PATH = ProjectInfoPropertyUtil.getProperty("interfacedFilePath", "./");
    public static final String PROJECT_NAME = ProjectInfoPropertyUtil.getProperty("project_name_en", "mj");
    public static String SERVER_IP = getLocalAddress();
    public static String HTTP_URL = "http://".concat(Cnst.SERVER_IP).concat(":").concat(ProjectInfoPropertyUtil.getProperty("httpUrlPort", "8086")).concat("/");
    public static String getLocalAddress(){
		String ip = "";
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return ip;
	}
    
    
    public static final String cid = ProjectInfoPropertyUtil.getProperty("cid", "46");;
    //redis配置
    public static final String REDIS_HOST = ProjectInfoPropertyUtil.getProperty("redis.host", "");
    public static final String REDIS_PORT = ProjectInfoPropertyUtil.getProperty("redis.port", "");
    public static final String REDIS_PASSWORD = ProjectInfoPropertyUtil.getProperty("redis.password", "");

    public static final String MINA_PORT = ProjectInfoPropertyUtil.getProperty("port", "");
    
    //用户服务地址
    public static final String GETUSER_URL = ProjectInfoPropertyUtil.getProperty("getUser_url","");
   
    public static final String rootPath = ProjectInfoPropertyUtil.getProperty("rootPath", "");

    public static final int MONEY_INIT = 2;//初始赠送给用户的房卡数
    public static Integer get_MONEY_INIT(String cid){
    	Integer initMoney = UserMapper.getInitMoney(cid);
    	if(initMoney == null){
    		//防止忘记设置数据库
    		return MONEY_INIT;
    	}
    	return initMoney;
    }
    //开房选项中的是否
    public static final int YES = 1;
    public static final int NO = 2;
    

    public static final long ROOM_OVER_TIME = 3*60*60*1000;//房间定时3小时解散
    public static final long ROOM_CREATE_DIS_TIME = 40*60*1000;//创建房间之后，40分钟解散
    public static final long ROOM_DIS_TIME = 5*60*1000;//玩家发起解散房间之后，5分钟自动解散
//    public static final long ROOM_DIS_TIME = 1*1000;//玩家发起解散房间之后，5分钟自动解散
    public static final String CLEAN_3 = "0 0 3 * * ?";
    public static final String CLEAN_EVERY_HOUR = "0 0 0/1 * * ?";
    public static final String COUNT_EVERY_TEN_MINUTE = "0 0/1 * * * ?";
    public static final long BACKFILE_STORE_TIME = 3*24*60*60*1000;//回放文件保存时间
   
    
    //玩家数据更新间隔时间
    public static long updateDiffTime =3*24*3600*1000;
    //测试时间
//    public static final long ROOM_OVER_TIME = 60*1000;//
//    public static final long ROOM_CREATE_DIS_TIME = 20*1000;
//    public static final long ROOM_DIS_TIME = 10*1000;
//	public static final String CLEAN_3 = "0/5 * * * * ?";
//	public static final String CLEAN_EVERY_HOUR = "0/5 * * * * ?";
//    public static final String COUNT_EVERY_TEN_MINUTE = "0/1 * * * * ?";
//    public static final long BACKFILE_STORE_TIME = 60*1000;//回放文件保存时间
//    public static final int PLAYOVER_LIFE_TIME = 60;//战绩保存时间
    

    public static final int ROOM_LIFE_TIME_CREAT = (int) ((ROOM_OVER_TIME/1000)+200);//创建时，5小时，redis用
    public static final int ROOM_LIFE_TIME_DIS = (int) ((ROOM_DIS_TIME/1000)+200);//解散房间时，300s，redis用
    public static final int ROOM_LIFE_TIME_COMMON = (int) ((ROOM_CREATE_DIS_TIME/1000)+200);//正常开局存活时间，redis用
    public static final int OVERINFO_LIFE_TIME_COMMON = (int) (10*60);//大结算 overInfo 存活时间
    public static final int PLAYOVER_LIFE_TIME =3*24*60*60;//战绩保存时间
    public static final int HUIFANG_LIFE_TIME = 30*60;//30分钟 回放写入文件后删除
    
    public static final int DIS_ROOM_RESULT = 1;

    public static final int DIS_ROOM_TYPE_1 = 1;//创建房间40分钟解散类型
    public static final int DIS_ROOM_TYPE_2 = 2;//玩家点击解散房间类型

    public static final int PAGE_SIZE = 10;
    



    public static final String USER_SESSION_USER_ID = "user_id";
    public static final String USER_SESSION_IP = "ip";
    
    

    //房间信息中的state
    // 1等待玩家入坐；2游戏中；3小结算  4大结算或已解散
    public static final int ROOM_STATE_CREATED = 1;
    public static final int ROOM_STATE_GAMIING = 2;
    public static final int ROOM_STATE_XJS = 3;
    public static final int ROOM_STATE_YJS = 4;
    public static final int ROOM_STATE_PIAO = 6;


    //房间类型
    public static final int ROOM_TYPE_1 = 1;//房主模式
    public static final int ROOM_TYPE_2 = 2;//自由模式
    public static final int ROOM_TYPE_3 = 3;//AA

    //风向表示
    public static final int WIND_EAST = 1;//东
    public static final int WIND_SOUTH = 2;//南
    public static final int WIND_WEST = 3;//西
    public static final int WIND_NORTH = 4;//北
    
    //记分方式
    public static final int BAOSANJIA = 1; // 包三家
    public static final int TINGSANJIAFU = 2; // 报听点炮三家付
    
    public static final int PLAY_TYPE_JINGDIAN = 1;//经典玩儿法
    public static final int PLAY_TYPE_DAIBAO = 2;//带宝玩儿法
    
    public static final int TING_STATE_1 = 1;//点听未出牌
    public static final int TING_STATE_2 = 2;//听完出牌
    

    //开房的局数对应消耗的房卡数
    public static final Map<Integer,Integer> moneyMap = new HashMap<>();
    static {
        moneyMap.put(2,1);
        moneyMap.put(4,2);
        moneyMap.put(8,4);
    }
    
    //以下三个是俱乐部的创建房间的房卡消耗
    public static final Map<Integer,Integer> clubMoneyInit_2 = new ConcurrentHashMap<Integer, Integer>();
    public static final Map<Integer,Integer> clubMoneyInit_3 = new ConcurrentHashMap<Integer, Integer>();
    public static final Map<Integer,Integer> clubMoneyInit_4 = new ConcurrentHashMap<Integer, Integer>();
    
    static {
    	clubMoneyInit_2.put(2,2);
    	clubMoneyInit_2.put(4,2);
    	clubMoneyInit_2.put(8,4);
    	

    	clubMoneyInit_3.put(2,3);
    	clubMoneyInit_3.put(4,3);
    	clubMoneyInit_3.put(8,6);
    	

    	clubMoneyInit_4.put(2,4);
    	clubMoneyInit_4.put(4,4);
    	clubMoneyInit_4.put(8,8);
    }
    
    public static Integer getClubRoomNeedMoney(int playerNum,int circleNum){
    	int need = 0;
    	switch (playerNum) {
		case 2:
			need = clubMoneyInit_2.get(circleNum);
			break;

		case 3:
			need = clubMoneyInit_3.get(circleNum);
			break;
		case 4:
			need = clubMoneyInit_4.get(circleNum);
			break;
		}
    	return need;
    }
    
    
    //玩家在线状态 state 
    public static final int PLAYER_LINE_STATE_INLINE = 1;//"inline"
    public static final int PLAYER_LINE_STATE_OUT = 2;//"out"
    
    //玩家进入或退出代开房间
    public static final int PLAYER_EXTRATYPE_ADDROOM = 1;//进入
    public static final int PLAYER_EXTRATYPE_EXITROOM = 2;//退出
    public static final int PLAYER_EXTRATYPE_JIESANROOM = 3;//解散
    public static final int PLAYER_EXTRATYPE_LIXIAN = 4;//离线
    public static final int PLAYER_EXTRATYPE_SHANGXIAN = 5;//上线
    public static final int PLAYER_EXTRATYPE_KAIJU = 6;//房间开局
    //玩家状态
    public static final int PLAYER_STATE_DATING = 1;//"dating"
    public static final int PLAYER_STATE_IN = 2;//"in"
    public static final int PLAYER_STATE_PREPARED = 3;//"prepared"
    public static final int PLAYER_STATE_GAME = 4;//"game"
    public static final int PLAYER_STATE_XJS = 5;//"xjs"
    

    //请求状态
    public static final int REQ_STATE_FUYI = -1;//敬请期待
    public static final int REQ_STATE_0 = 0;//非法请求
    public static final int REQ_STATE_1 = 1;//正常
    public static final int REQ_STATE_2 = 2;//余额不足
    public static final int REQ_STATE_3 = 3;//已经在其他房间中
    public static final int REQ_STATE_4 = 4;//房间不存在
    public static final int REQ_STATE_5 = 5;//房间人员已满
    public static final int REQ_STATE_6 = 6;//游戏中，不能退出房间
    public static final int REQ_STATE_7 = 7;//有玩家拒绝解散房间
    public static final int REQ_STATE_8 = 8;//玩家不存在（代开模式中，房主踢人用的）
    public static final int REQ_STATE_9 = 9;//接口id不符合，需请求大接口
    public static final int REQ_STATE_10 = 10;//代开房间创建成功
    public static final int REQ_STATE_11 = 11;//已经代开过10个了，不能再代开了
    public static final int REQ_STATE_12 = 12;//房间存在超过24小时解散的提示
    public static final int REQ_STATE_13 = 13;//房间40分钟未开局解散提示
    public static final int REQ_STATE_14 = 14;//ip不一致
    
  //以下是俱乐部的
    public static final int REQ_STATE_15 = 15;//已经在俱乐部里了
    public static final int REQ_STATE_16 = 16;//玩家加入俱乐部的数量已满
    public static final int REQ_STATE_17 = 17;//申请操作成功
    public static final int REQ_STATE_18 = 18;//已经操作过了
    public static final int REQ_STATE_19 = 19;//玩家不在当前俱乐部
    public static final int REQ_STATE_20 = 20;//当前创建的房间数量已满
    public static final int REQ_STATE_21 = 21;//俱乐部剩余房卡不足，不能创建房间
    public static final int REQ_STATE_22 = 22;//今日玩家房卡消耗已达上限
    public static final int REQ_STATE_23 = 23;//该俱乐部不存在

    //动作类型
    public static final int ACTION_TYPE_CHI = 1;
    public static final int ACTION_TYPE_PENG = 2;
    public static final int ACTION_TYPE_PENGGANG = 3;
    public static final int ACTION_TYPE_DIANGANG = 4;
    public static final int ACTION_TYPE_ANGANG = 5;
    public static final int ACTION_TYPE_FAPAI = 6;
    public static final int ACTION_TYPE_CHUPAI = 7;
    public static final int ACTION_TYPE_GUO = 8;
    public static final int ACTION_TYPE_HU = 9;
    public static final int ACTION_TYPE_TING = 10;
    public static final int ACTION_TYPE_BAOLOU = 11;
    public static final int ACTION_TYPE_BAOTING= 12;
    
    public static final int ACTION_BIANMA_TING = 174;//听的行为编码
    public static final int ACTION_BIANMA_BAOLOU = 231;//暴露的行为编码
    public static final int ACTION_BIANMA_BAOTING = 232;//报听的行为编码
    
    //检测类型
    public static final int CHECK_TYPE_ZIJIMO = 1;
    public static final int CHECK_TYPE_BIERENCHU = 2;
    public static final int CHECK_TYPE_HAIDIANPAI = 3;
    public static final int CHECK_TYPE_QIANGGANG = 4;
    public static final int CHECK_TYPE_TIANHU = 5;
//  public static final int CHECK_TYPE_DIHU = 6;


    //退出类型
    public static final int EXIST_TYPE_EXIST = 1;//"exist"
    public static final int EXIST_TYPE_DISSOLVE = 2;//"dissolve";

    // 项目根路径
    public static String ROOTPATH = "";
    
    public static final String REDIS_PREFIX = ProjectInfoPropertyUtil.getProperty("redis.prefix","LINKOU");//会清理的数据
    public static final String REDIS_RECORD_PREFIX = ProjectInfoPropertyUtil.getProperty("redis.record_prefix","LINK");//不会清理的数据
    
    //redis存储的key的不同类型的前缀
    public static final String REDIS_PREFIX_ROOMMAP = REDIS_PREFIX+"_ROOM_MAP_";//房间信息
    public static final String REDIS_PREFIX_OPENIDUSERMAP = REDIS_PREFIX+"_OPENID_USERID_MAP_";//openId-user数据
    public static final String REDIS_PREFIX_USER_ID_USER_MAP = REDIS_PREFIX+"_USER_ID_USER_MAP_";//通过userId获取用户
    
    //代开房间列表
    public static final String REDIS_PREFIX_DAI_ROOM_LIST = REDIS_PREFIX+"_DAIKAI_ROOM_LIST_";//通过代开房间列表
    //redis中通知的key
    public static final String NOTICE_KEY = REDIS_PREFIX+"_NOTICE_KEY";
    
    public static final String PROJECT_PREFIX = REDIS_PREFIX+"_*";
    
    public static final String REDIS_ONLINE_NUM_COUNT = REDIS_PREFIX+"_ONLINE_NUM_";
    
    public static final String REDIS_HEART_PREFIX = REDIS_PREFIX+"_HEART_USERS_MAP";
    
    public static final String REDIS_HUIFANG_MAP = REDIS_PREFIX+"_HUIFANG_MAP_";//房间号+时间戳+小局
    
    
    //俱乐部
    //俱乐部
    //清理的
    public static final String REDIS_CLUB_CLUBMAP = REDIS_PREFIX.concat("_CLUB_MAP_");//俱乐部信息--清理,防止停服的时候代理后端从房卡
    public static final String REDIS_CLUB_ROOM_LIST = REDIS_PREFIX.concat("_CLUB_MAP_LIST_");//存放俱乐部未开局房间信息--这个可以清
    //不清理的
    public static final String REDIS_CLUB_PLAY_RECORD_PREFIX = REDIS_RECORD_PREFIX.concat("_CLUB_PLAY_RECORD_");//房间战绩
    public static final String REDIS_CLUB_PLAY_RECORD_PREFIX_ROE_USER =  REDIS_RECORD_PREFIX.concat("_CLUB_PLAY_RECORD_FOR_USER_");//玩家字段
    public static final String REDIS_CLUB_TODAYSCORE_ROE_USER = REDIS_RECORD_PREFIX.concat("_CLUB_TODAYSCORE_FOR_USER_");//统计玩家今日分数
    public static final String REDIS_CLUB_TODAYJUNUM_ROE_USER = REDIS_RECORD_PREFIX.concat("_CLUB_TODAYJUNUM_FOR_USER_");//统计玩家今日局数
    public static final String REDIS_CLUB_ACTIVE_NUM = REDIS_RECORD_PREFIX.concat("_CLUB_ACTIVE_NUM_");//今天活跃人数
    public static final String REDIS_CLUB_TODAYKAI_NUM = REDIS_RECORD_PREFIX.concat("_CLUB_TODAYKAI_NUM_");//今天开局数

    public static final int REDIS_CLUB_DIE_TIME = 3*24*60*60;//玩家战绩和俱乐部每天活跃保存时间
    public static final int REDIS_CLUB_PLAYERJUNUM_TIME =2* 24*60*60;//玩家今日局数和昨日局数保存时间
    //这个字段不清理，存放玩家战绩，定时任务定期清理内容
    public static final String REDIS_PLAY_RECORD_PREFIX = REDIS_RECORD_PREFIX+"_PLAY_RECORD_";//房间战绩
    public static final String REDIS_PLAY_RECORD_PREFIX_ROE_USER = REDIS_RECORD_PREFIX+"_PLAY_RECORD_FOR_USER_";//玩家字段
    public static final String REDIS_PLAY_RECORD_PREFIX_ROE_DAIKAI = REDIS_RECORD_PREFIX+"_PLAY_RECORD_FOR_DAIKAI_";//代开房间
    public static final String REDIS_PLAY_RECORD_PREFIX_OVERINFO = REDIS_PREFIX+"_PLAY_RECORD_OVERINFO_";//大结算
    
   
    public static Map<String,String> ROUTE_MAP = new ConcurrentHashMap<String, String>();
    public static Map<String,String> ROUTE_MAP_SEND = new ConcurrentHashMap<String, String>();
    //FIX ME 别多打空格 生成文件会有问题
    static{
    	ROUTE_MAP_SEND.put("a","interfaceId");
    	ROUTE_MAP_SEND.put("b","state");
    	ROUTE_MAP_SEND.put("c","message");
    	ROUTE_MAP_SEND.put("d","info");
    	ROUTE_MAP_SEND.put("e","others");
    	ROUTE_MAP_SEND.put("f","page");
    	ROUTE_MAP_SEND.put("g","infos");
    	ROUTE_MAP_SEND.put("h","pages");
    	ROUTE_MAP_SEND.put("i","connectionInfo");
    	ROUTE_MAP_SEND.put("j","help");
    	ROUTE_MAP_SEND.put("k","userId");
    	ROUTE_MAP_SEND.put("l","content");
    	ROUTE_MAP_SEND.put("m","tel");
    	ROUTE_MAP_SEND.put("n","roomType");
    	ROUTE_MAP_SEND.put("o","type");
    	ROUTE_MAP_SEND.put("p","clubId");
    	ROUTE_MAP_SEND.put("q","clubName");
    	ROUTE_MAP_SEND.put("r","clubUserName");
    	ROUTE_MAP_SEND.put("s","allNums");
    	ROUTE_MAP_SEND.put("t","maxNums");
    	ROUTE_MAP_SEND.put("u","freeStart");
    	ROUTE_MAP_SEND.put("v","freeEnd");
    	ROUTE_MAP_SEND.put("w","clubMoney");
    	ROUTE_MAP_SEND.put("x","cardQuota");
    	ROUTE_MAP_SEND.put("y","juNum");
    	ROUTE_MAP_SEND.put("z","used");
    	ROUTE_MAP_SEND.put("A","roomId");
    	ROUTE_MAP_SEND.put("B","reqState");
    	ROUTE_MAP_SEND.put("C","playerNum");
    	ROUTE_MAP_SEND.put("D","money");
    	ROUTE_MAP_SEND.put("E","playStatus");
    	ROUTE_MAP_SEND.put("F","position");
    	ROUTE_MAP_SEND.put("G","userInfo");
    	ROUTE_MAP_SEND.put("H","playType");
    	ROUTE_MAP_SEND.put("I","wsw_sole_action_id");
    	ROUTE_MAP_SEND.put("J","roomInfo");
    	ROUTE_MAP_SEND.put("K","lastNum");
    	ROUTE_MAP_SEND.put("L","roomIp");
    	ROUTE_MAP_SEND.put("M","ip");
    	ROUTE_MAP_SEND.put("N","xjst");
    	ROUTE_MAP_SEND.put("O","score");
    	ROUTE_MAP_SEND.put("P","userName");
    	ROUTE_MAP_SEND.put("Q","userImg");
    	ROUTE_MAP_SEND.put("R","joinIndex");
    	ROUTE_MAP_SEND.put("S","gender");
    	ROUTE_MAP_SEND.put("T","createTime");
    	ROUTE_MAP_SEND.put("U","circleNum");
    	ROUTE_MAP_SEND.put("V","openId");
    	ROUTE_MAP_SEND.put("W","cId");
    	ROUTE_MAP_SEND.put("X","currentUser");
    	ROUTE_MAP_SEND.put("Y","anotherUsers");
    	ROUTE_MAP_SEND.put("Z","version");
    	ROUTE_MAP_SEND.put("aa","userAgree");
    	ROUTE_MAP_SEND.put("ab","notice");
    	ROUTE_MAP_SEND.put("ac","actNum");
    	ROUTE_MAP_SEND.put("ad","exState");
    	ROUTE_MAP_SEND.put("ae","pais");
    	ROUTE_MAP_SEND.put("af","xiaoJuNum");
    	ROUTE_MAP_SEND.put("ag","zhuangPlayer");
    	ROUTE_MAP_SEND.put("ah","dissolveTime");
    	ROUTE_MAP_SEND.put("ai","othersAgree");
    	ROUTE_MAP_SEND.put("aj","dissolveRoom");
    	ROUTE_MAP_SEND.put("ak","continue");
    	ROUTE_MAP_SEND.put("al","nextAction");
    	ROUTE_MAP_SEND.put("am","nextActionUserId");
    	ROUTE_MAP_SEND.put("an","currAction");
    	ROUTE_MAP_SEND.put("ao","currActionUserId");
    	ROUTE_MAP_SEND.put("ap","lastAction");
    	ROUTE_MAP_SEND.put("aq","lastActionUserId");
    	ROUTE_MAP_SEND.put("ar", "agree");
    	ROUTE_MAP_SEND.put("as", "idx");
    	ROUTE_MAP_SEND.put("at", "date");
    	ROUTE_MAP_SEND.put("au", "extra");
    	ROUTE_MAP_SEND.put("av", "extraType");  	
    	ROUTE_MAP_SEND.put("aw", "startPosition");
    	ROUTE_MAP_SEND.put("ax", "x_index");
    	ROUTE_MAP_SEND.put("ay", "y_index");
    	ROUTE_MAP_SEND.put("az", "distance");
    	ROUTE_MAP_SEND.put("ba", "xiaoJuInfo");
    	ROUTE_MAP_SEND.put("bb", "backUrl");   	
    	//麻将特有
    	ROUTE_MAP_SEND.put("ca", "scoreType");
    	ROUTE_MAP_SEND.put("cb", "chuList");
    	ROUTE_MAP_SEND.put("cc", "actionList");
    	ROUTE_MAP_SEND.put("cd", "currMJNum");
    	ROUTE_MAP_SEND.put("ce", "lastChuUserId");
    	ROUTE_MAP_SEND.put("cf", "lastFaUserId");
    	ROUTE_MAP_SEND.put("cg", "action");
    	ROUTE_MAP_SEND.put("ch", "zhuangNum");
    	ROUTE_MAP_SEND.put("ci", "huScore");
    	ROUTE_MAP_SEND.put("cj", "huNum");
    	ROUTE_MAP_SEND.put("ck", "dianNum");
    	ROUTE_MAP_SEND.put("cl", "isWin");
    	ROUTE_MAP_SEND.put("cm", "isDian");
    	ROUTE_MAP_SEND.put("cn", "winInfo");
    	ROUTE_MAP_SEND.put("co", "dingHunPai");
    	ROUTE_MAP_SEND.put("cp", "toUserId");
    	ROUTE_MAP_SEND.put("cq", "ziMoNum");
    	ROUTE_MAP_SEND.put("cr", "gangScore"); 	
    	//桦甸特有
    	ROUTE_MAP_SEND.put("ea", "playerNum");
    	ROUTE_MAP_SEND.put("eb", "piao");
    	ROUTE_MAP_SEND.put("ec", "baoPai");
    	ROUTE_MAP_SEND.put("ed", "ting");
    	ROUTE_MAP_SEND.put("ee", "tingList");
    	ROUTE_MAP_SEND.put("ef", "baoLouHu");
   
    	Iterator<String> i = ROUTE_MAP_SEND.keySet().iterator();
    	while(i.hasNext()){
    		String key = i.next();
    		ROUTE_MAP.put(ROUTE_MAP_SEND.get(key), key);
    	}

    }

    
    public static int[] CHI35 = new int[]{1,2,3};
    public static int[] CHI36 = new int[]{2,3,4};
    public static int[] CHI37 = new int[]{3,4,5};
    public static int[] CHI38 = new int[]{4,5,6};
    public static int[] CHI39 = new int[]{5,6,7};
    public static int[] CHI40 = new int[]{6,7,8};
    public static int[] CHI41 = new int[]{7,8,9};
    
    public static int[] CHI42 = new int[]{10,11,12};
    public static int[] CHI43 = new int[]{11,12,13};
    public static int[] CHI44 = new int[]{12,13,14};
    public static int[] CHI45 = new int[]{13,14,15};
    public static int[] CHI46 = new int[]{14,15,16};
    public static int[] CHI47 = new int[]{15,16,17};
    public static int[] CHI48 = new int[]{16,17,18};
    
    public static int[] CHI49 = new int[]{19,20,21};
    public static int[] CHI50 = new int[]{20,21,22};
    public static int[] CHI51 = new int[]{21,22,23};
    public static int[] CHI52 = new int[]{22,23,24};
    public static int[] CHI53 = new int[]{23,24,25};
    public static int[] CHI54 = new int[]{24,25,26};
    public static int[] CHI55 = new int[]{25,26,27};

    public static int[] CHI56 = new int[]{32,33,34};
    public static Map<Integer,int[]> chiMap = new ConcurrentHashMap<Integer, int[]>();
    static{
    	chiMap.put(35, CHI35);
    	chiMap.put(36, CHI36);
    	chiMap.put(37, CHI37);
    	chiMap.put(38, CHI38);
    	chiMap.put(39, CHI39);
    	chiMap.put(40, CHI40);
    	chiMap.put(41, CHI41);
    	chiMap.put(42, CHI42);
    	chiMap.put(43, CHI43);
    	chiMap.put(44, CHI44);
    	chiMap.put(45, CHI45);
    	chiMap.put(46, CHI46);
    	chiMap.put(47, CHI47);
    	chiMap.put(48, CHI48);
    	chiMap.put(49, CHI49);
    	chiMap.put(50, CHI50);
    	chiMap.put(51, CHI51);
    	chiMap.put(52, CHI52);
    	chiMap.put(53, CHI53);
    	chiMap.put(54, CHI54);
    	chiMap.put(55, CHI55);
    	chiMap.put(56, CHI56);
    }
    
    //大于50的，是乘法项
    public static final int HU_TYPE_PINGHU = 1;			//平胡
    public static final int HU_TYPE_PIAOHU = 2;     //飘胡
    public static final int HU_TYPE_QIDUI = 3;     //七对
    public static final int HU_TYPE_HAOQI = 4;      //豪七
    public static final int HU_TYPE_QINGYISE = 5; //清一色
    public static final int HU_TYPE_TIANHU = 6;        //天胡
    public static final int HU_TYPE_DIHU = 7;           //地胡
    public static final int HU_TYPE_BAOLOU = 8;         //暴露
    
    public static final int HU_TYPE_ZIMO = 9;          //自摸
    public static final int HU_TYPE_DIANPAO = 10;          //点炮
    public static final int HU_TYPE_JIAPIAO = 11;      //加飘
    public static final int HU_TYPE_TONGBAO = 12; //通宝
    
    

	public static final ArrayList<Integer> ACTION_YI_JIU =new ArrayList<Integer>();
    static{
    	//吃的1-9
    	ACTION_YI_JIU.add(35);
    	ACTION_YI_JIU.add(41);
    	ACTION_YI_JIU.add(42);
    	ACTION_YI_JIU.add(48);
    	ACTION_YI_JIU.add(49);
    	ACTION_YI_JIU.add(55);
    	//碰的1-9
    	ACTION_YI_JIU.add(57);
    	ACTION_YI_JIU.add(65);
    	ACTION_YI_JIU.add(66);
    	ACTION_YI_JIU.add(74);
    	ACTION_YI_JIU.add(75);
    	ACTION_YI_JIU.add(83);
    	//杠的1-9
    	ACTION_YI_JIU.add(91);
    	ACTION_YI_JIU.add(99);
    	ACTION_YI_JIU.add(100);
    	ACTION_YI_JIU.add(108);
    	ACTION_YI_JIU.add(109);
    	ACTION_YI_JIU.add(117);
    	
    	//碰的中
    	ACTION_YI_JIU.add(88);
    	//杠的中
    	ACTION_YI_JIU.add(122);
    }
    public static final ArrayList<Integer> PAI_YI_JIU =new ArrayList<Integer>();

    static{
    	PAI_YI_JIU.add(1);
    	PAI_YI_JIU.add(9);
    	PAI_YI_JIU.add(10);
    	PAI_YI_JIU.add(18);
    	PAI_YI_JIU.add(19);
    	PAI_YI_JIU.add(27);
    }

	//制造假数据的房间id  从10000开始
	public static  Integer TEST_ROOMID = 100000;
	public static  Integer TEST_CLUB_ROOMID = 1000000;
	public static  Integer TEST_DAIKAI_ROOMID = 400000;  
    public static final int TEST_PLAYOVER_LIFE_TIME =3*60*60;//测试战绩保存3个小时时间

	public static final int MINGGANG = 1;
	public static final int ANGANG = 2;
	
	public static final int CHECK_PUTONGTING = 1;
	public static final int CHECK_CHITING = 2;
	public static final int CHECK_PENGTING = 3;
	
	public static final int DIYICI_HUANBAO = 1;
	public static final int ZHENGCHANG_HUANBAO = 2;
	public static final int BU_HUANBAO = 3;
}
