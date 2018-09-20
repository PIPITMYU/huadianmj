package com.yzt.logic.mj.function;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.yzt.logic.mj.domain.Action;
import com.yzt.logic.mj.domain.ClubInfo;
import com.yzt.logic.mj.domain.DissolveRoom;
import com.yzt.logic.mj.domain.Player;
import com.yzt.logic.mj.domain.RoomResp;
import com.yzt.logic.util.BackFileUtil;
import com.yzt.logic.util.Cnst;
import com.yzt.logic.util.MahjongUtils;
import com.yzt.logic.util.RoomUtil;
import com.yzt.logic.util.GameUtil.JieSuan;
import com.yzt.logic.util.GameUtil.StringUtils;
import com.yzt.logic.util.JudegHu.checkHu.Hulib;
import com.yzt.logic.util.redis.RedisUtil;
import com.yzt.netty.client.WSClient;
import com.yzt.netty.util.MessageUtils;

/**
 * Created by Administrator on 2017/7/13. 游戏中
 */

public class GameFunctions extends TCPGameFunctions {
	final static Object object = new Object();

	/**
	 * 用户点击准备，用在小结算那里，
	 * 
	 * @param session
	 * @param readData
	 */
	public synchronized static void interface_100200(WSClient channel, Map<String, Object> readData) {
		logger.info("准备,interfaceId -> 100200");

		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomId"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId));
		if (room == null) {
			illegalRequest(interfaceId, channel);
			return;
		}
		Player currentPlayer = null;
		List<Player> players = RedisUtil.getPlayerList(room);
		for (Player p : players) {
			if (p.getUserId().equals(userId)) {
				currentPlayer = p;
				break;
			}
		}

		if (room.getState() == Cnst.ROOM_STATE_GAMIING) {
			return;
		}
		if (currentPlayer == null) {
			return;
		}
		// TODO
		currentPlayer.initPlayer(currentPlayer.getRoomId(), Cnst.PLAYER_STATE_PREPARED, currentPlayer.getScore());

		boolean allPrepared = true;

		for (Player p : players) {
			if (!p.getPlayStatus().equals(Cnst.PLAYER_STATE_PREPARED)) {
				allPrepared = false;
			}
		}

		if (allPrepared && players != null && players.size() == room.getPlayerNum()) {
			if (room.getRoomType() == Cnst.ROOM_TYPE_2) {
				MessageFunctions.interface_100112(null, room, Cnst.PLAYER_EXTRATYPE_KAIJU);
			}
			// 如果选择带飘选项--进入选飘阶段
			if (room.getPlayType().equals(1)) {
				boolean needCheck=false;
				for (Player player : players) {
					if(player.getPiao()!=null && !player.getPiao().equals(4)){
						needCheck=true;
						break;
					}
				}
				//说明四个人都选的飘到底
				if(needCheck){
					room.setState(Cnst.ROOM_STATE_PIAO);
					// 更新缓存数据
					RedisUtil.setPlayersList(players);
					RedisUtil.updateRedisData(room, null);
					Map<String, Object> info = new HashMap<String, Object>();
					List<Map<String, Object>> userInfo = new ArrayList<Map<String, Object>>();
					for (Player p : players) {
						Map<String, Object> i = new HashMap<String, Object>();
						i.put("userId", p.getUserId());
						i.put("playStatus", p.getPlayStatus());
						i.put("piao", p.getPiao());
						userInfo.add(i);
					}
					Map<String, Object> roominfo = new HashMap<String, Object>();
					roominfo.put("state", room.getState());
					info.put("roomInfo", roominfo);
					info.put("userInfo", userInfo);
					JSONObject result = getJSONObj(interfaceId, 1, info);
					for (Player p : players) {
						WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
						if (ws == null)
							continue;
						MessageUtils.sendMessage(ws, result.toJSONString());
					}
					return;
				}
			}
			startGame(room, players);
			BackFileUtil.save(interfaceId, room, players, null, null);// 写入文件内容
		}
		Map<String, Object> info = new HashMap<String, Object>();
		List<Map<String, Object>> userInfo = new ArrayList<Map<String, Object>>();
		for (Player p : players) {
			Map<String, Object> i = new HashMap<String, Object>();
			i.put("userId", p.getUserId());
			i.put("playStatus", p.getPlayStatus());
			if(room.getPlayType() == 1){
				i.put("piao", p.getPiao());
			}
			userInfo.add(i);
		}
		info.put("userInfo", userInfo);
		Map<String, Object> roominfo = new HashMap<String, Object>();
		roominfo.put("state", room.getState());
		info.put("roomInfo", roominfo);
		for (Player p : players) {
			WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
			if (ws == null)
				continue;
			if (room.getState() == Cnst.ROOM_STATE_GAMIING) {
				info.put("pais", p.getCurrentMjList());
				info.put("nextActionUserId", room.getNextActionUserId());
				info.put("nextAction", room.getNextAction());
			}
			JSONObject result = getJSONObj(interfaceId, 1, info);
			MessageUtils.sendMessage(ws, result.toJSONString());
		}
		RedisUtil.setPlayersList(players);
		RedisUtil.updateRedisData(room, null);
	}

	/**
	 * 飘接口 房间飘状态进入
	 * 
	 * @param channel
	 * @param readData
	 */
	public  synchronized static  void interface_100202(WSClient channel, Map<String, Object> readData) {
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomId"));
		Integer userId = StringUtils.parseInt(readData.get("userId"));
		Integer piao = StringUtils.parseInt(readData.get("piao"));
		if (userId == null || roomId == null || piao == null) {
			return;
		}
		Player player = RedisUtil.getPlayerByUserId(userId + "");
		// 没有此玩家或者玩家没有房间号
		if (player == null || player.getRoomId() == null) {
			return;
		}
		// 房间号不一致
		if (!player.getRoomId().equals(roomId)) {
			return;
		}
		player.setPiao(piao);
		// 更新缓存
		RedisUtil.setPlayerByUserId(player);
		// 获取房间信息
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId));
		List<Player> players = RedisUtil.getPlayerList(room);
		Boolean allPiao = true;
		Integer piaoNum = 0;
		for (Player p : players) {
			if (p.getPiao() == 1) {
				allPiao = false;
			} else {
				piaoNum++;
			}
		}
		if (allPiao) {
			GameFunctions.startGame(room, players);
			BackFileUtil.save(100200, room, players, null, null);// 写入文件内容
		}
		// 封装玩家信息
		Map<String, Object> info = new HashMap<String, Object>();
		List<Map<String, Object>> userInfo = new ArrayList<Map<String, Object>>();
		for (Player p : players) {
			Map<String, Object> i = new HashMap<String, Object>();
			i.put("userId", p.getUserId());
			i.put("piao", p.getPiao());
//			i.put("playStatus", p.getPlayStatus());
			userInfo.add(i);
		}
		info.put("userInfo", userInfo);
		// 封装房间信息
		Map<String, Object> roominfo = new HashMap<String, Object>();
		roominfo.put("state", room.getState());
		info.put("roomInfo", roominfo);
		info.put("num", piaoNum);
		for (Player p : players) {
			WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
			if (ws == null)
				continue;
			if (room.getState() == Cnst.ROOM_STATE_GAMIING) {
				// 加入庄的玩家id
				info.put("nextActionUserId", room.getZhuangId());
				info.put("nextAction", room.getNextAction());
				info.put("pais", p.getCurrentMjList());
			}
			JSONObject result = getJSONObj(interfaceId, 1, info);
			MessageUtils.sendMessage(ws, result.toJSONString());
		}
		RedisUtil.setPlayersList(players);
		RedisUtil.updateRedisData(room, null);
	}

	/**
	 * 开局发牌
	 * 
	 * @param roomId
	 */
	public static void startGame(RoomResp room, List<Player> players) {
		// 局数统计
		room.setXiaoJuNum(room.getXiaoJuNum() == null ? 1 : room.getXiaoJuNum() + 1);
		room.setXjst(System.currentTimeMillis());
		room.setState(Cnst.ROOM_STATE_GAMIING);

		// 获得所需要的牌型
		room.setCurrentMjList(MahjongUtils.getPais(room));

		// 发牌
		Player zhuangPlayer = null;
		for (Player p : players) {
			p.setPlayStatus(Cnst.PLAYER_STATE_GAME);// 游戏中..

			if (room.getZhuangId().equals(p.getUserId())) {

				zhuangPlayer = p;
				p.setZhuangNum(p.getZhuangNum() == null ? 1 : p.getZhuangNum() + 1);// 坐庄的次数
				p.setCurrentMjList(MahjongUtils.faPai(room.getCurrentMjList(), 14));
			} else {
				p.setCurrentMjList(MahjongUtils.faPai(room.getCurrentMjList(), 13));
			}

		}
		// 定宝牌,开局的时候随便定一个就行
		room.getCurrentMjList().remove(0);//去掉一个宝牌
		room.setBaoPai(0);
		room.setFirstBaoPai((int)(Math.random()*27)+1);
		room.setDiHu(true);
		// 3.1看庄家有没有暗杠.带混检测, 有没有胡牌等.
		List<Integer> actionList = MahjongUtils.checkActionList(zhuangPlayer, zhuangPlayer.getCurrentMjList().get(13), room, Cnst.CHECK_TYPE_TIANHU,
				false);
		room.setNextAction(actionList);// 动作集合发给前端的.
		room.setNextActionUserId(room.getZhuangId());
		// 设置最后发牌人
		room.setLastFaPai(zhuangPlayer.getCurrentMjList().get(13));
		room.setLastFaPaiUserId(room.getZhuangId());
		room.setWindPosition(zhuangPlayer.getUserId());
		if (room.getXiaoJuNum() == 1) {
			// 获取此俱乐部当天活跃总人数
			if (String.valueOf(room.getRoomId()).length() == 7) {// 说明是俱乐部
				// 今日活跃数 key: cid value ：userId的集合
				Long timesmorning = StringUtils.getTimesmorning();
				Long scard = RedisUtil.scard(room.getClubId() + "_".concat(timesmorning + ""));
				int dieTime = Cnst.REDIS_CLUB_DIE_TIME;
				if (scard == null || scard == 0l) {// 当天没人,有人最少为5
					// 创建一个并设置过期时间(其中1l为假数据)--昨日和前日
					// 假数据主要是为了设置过期时间--此时间只设置一次
					RedisUtil.sadd(room.getClubId() + "_".concat(timesmorning + ""), 1l, dieTime);
					for (Long userId : room.getPlayerIds()) {
						RedisUtil.sadd(room.getClubId() + "_".concat(timesmorning + ""), userId, null);
					}
				} else {// 有人
					for (Long userId : room.getPlayerIds()) {
						RedisUtil.sadd(room.getClubId() + "_".concat(timesmorning + ""), userId, null);
					}
				}
				// 今日俱乐部局数 --昨日和前日
				Integer clubId = room.getClubId();
				Integer todayJuNum = RedisUtil.getTodayJuNum(clubId + "".concat(timesmorning + ""));
				if (todayJuNum == null || todayJuNum == 0) {
					RedisUtil.setTodayJuNum(clubId + "".concat(timesmorning + ""), 1, dieTime);
				} else {
					RedisUtil.setTodayJuNum(clubId + "".concat(timesmorning + ""), 1 + todayJuNum, dieTime);
				}
				Long[] playerIds = room.getPlayerIds();
				// 今日玩家局数 --保存一天
				Integer juNum = null;
				for (Long playerId : playerIds) {
					// key clubId+userId+今天早上时间
					juNum = RedisUtil.getObject(
							Cnst.REDIS_CLUB_TODAYJUNUM_ROE_USER.concat(clubId + "_").concat(playerId + "").concat(timesmorning + ""), Integer.class);
					if (juNum == null || juNum == 0) {
						RedisUtil.setObject(Cnst.REDIS_CLUB_TODAYJUNUM_ROE_USER.concat(clubId + "_").concat(playerId + "").concat(timesmorning + ""),
								1, Cnst.REDIS_CLUB_PLAYERJUNUM_TIME);
					} else {
						RedisUtil.setObject(Cnst.REDIS_CLUB_TODAYJUNUM_ROE_USER.concat(clubId + "_").concat(playerId + "").concat(timesmorning + ""),
								juNum + 1, Cnst.REDIS_CLUB_PLAYERJUNUM_TIME);
					}
				}
				RedisUtil.hdel(Cnst.REDIS_CLUB_ROOM_LIST.concat(String.valueOf(room.getClubId())), String.valueOf(room.getRoomId()));
			}

			RoomUtil.addRoomToDB(room);
			RoomUtil.removeFreeRoomTask(StringUtils.parseLong(room.getRoomId()));
		}
	}

	/**
	 * 出牌 行为编码(游戏内主逻辑)
	 * 
	 * @param wsClient
	 * @param readData
	 */
	public synchronized static void interface_100201(WSClient channel, Map<String, Object> readData) {
		logger.info("游戏内主逻辑,interfaceId -> 100201");
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer action = StringUtils.parseInt(readData.get("action")); // 行为编号,牌的信息
		if (action == -4) {
			return;
		}
		Integer roomId = StringUtils.parseInt(readData.get("roomId")); // 房间号
		Long userId = StringUtils.parseLong(readData.get("userId")); // 玩家ID\
		Integer wsw_sole_action_id = StringUtils.parseInt(readData.get("wsw_sole_action_id"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId));
		if (!room.getNextActionUserId().equals(userId)) {
			return;
		}
		List<Player> players = RedisUtil.getPlayerList(room);

		Player currentPlayer = null;
		for (Player p : players) {
			if (p.getUserId().equals(userId)) {
				currentPlayer = p;
				break;
			}
		}
		if (currentPlayer == null) {
			return;
		}
		if (room.getNextAction() != null) {
			if (room.getNextAction().contains(501)) {
				// 检测用户手牌是否包含这张牌
				if (!currentPlayer.getCurrentMjList().contains(action)) {
					logger.info("这里 TM 手牌没这个啊");
					illegalRequest(interfaceId, channel);
					return;
				}
			} else {
				if (!room.getNextAction().contains(action)) {
					logger.info("这里 TM 动作不对啊");
					illegalRequest(interfaceId, channel);
					return;
				}
			}

		}
		if (!room.getWsw_sole_action_id().equals(wsw_sole_action_id)) {
			return;
		}
		room.setWsw_sole_action_id(room.getWsw_sole_action_id() + 1);
		boolean huanBao = false;
		// 设置上个吃碰杠动作统计
		if (action != null && action >= 1 && action <= 126) {
			room.setLastAction(action);
			room.setLastActionUserId(userId);
			if (action >= 35 && action <= 126) {
				// 清空 过 集合
				room.getGuoUserIds().clear();
			}
		}
		List<Integer> currentMjList = currentPlayer.getCurrentMjList();
		Action ac = null;
		// 判断动作
		in: if (action == -1) {// 发牌
			logger.info("-1 请求发牌!");
			List<Integer> faPai = MahjongUtils.faPai(room.getCurrentMjList(), 1);
			room.setWindPosition(userId);
			if (faPai == null) {// 荒庄
				ac = new Action(Cnst.ACTION_TYPE_FAPAI, null, userId, null, null);
				room.setState(Cnst.ROOM_STATE_XJS);
				room.setHuangZhuang(true);
				break in;
			}
			room.getGuoUserIds().clear();
			// 海底牌玩家不用出牌
			if (room.getCurrentMjList().size() < room.getPlayerNum()) {
				Integer haiDiPai = faPai.get(0);
				room.setLastFaPai(haiDiPai);
				room.setLastFaPaiUserId(userId);
				currentMjList.add(haiDiPai);
				ac = new Action(Cnst.ACTION_TYPE_FAPAI, action, userId, null, haiDiPai);
				List<Integer> currentPlayerActionList = MahjongUtils
						.checkActionList(currentPlayer, haiDiPai, room, Cnst.CHECK_TYPE_HAIDIANPAI, false);
				if (currentPlayerActionList != null && currentPlayerActionList.size() > 0) {
					room.setNextAction(currentPlayerActionList);
					room.setNextActionUserId(userId);
				} else {
					getNextFaPaiHaiDi(room, userId);
				}
			} else {
				Integer pai = faPai.get(0);
				// 添加玩家手牌
				currentMjList.add(pai);
				ac = new Action(Cnst.ACTION_TYPE_FAPAI, action, userId, null, pai);
				List<Integer> currentPlayerActionList = MahjongUtils.checkActionList(currentPlayer, pai, room, Cnst.CHECK_TYPE_ZIJIMO, false);

				// 设置最后发牌人
				room.setLastFaPai(pai);
				room.setLastFaPaiUserId(userId);
				room.setNextAction(currentPlayerActionList);
				room.setNextActionUserId(userId);
			}
			// 过
		} else if (action == 0) {
			logger.info("过!!!!!!");
			ac = new Action(Cnst.ACTION_TYPE_GUO, action, userId, null, null);
			if (room.getIsQiangGangHu() != null && room.getIsQiangGangHu() == true) {
				// 抢杠胡点过

				List<Integer> nextAction = new ArrayList<Integer>();
				room.getQiangGangHu().remove(userId);
				if (room.getQiangGangHu().size() == 0) {
					room.setIsQiangGangHu(false);
					nextAction.add(-1);
					room.setNextAction(nextAction);
					room.setNextActionUserId(room.getLastActionUserId());
				} else {
					nextAction.add(500);
					nextAction.add(0);
					room.setNextAction(nextAction);
					room.setNextActionUserId(room.getQiangGangHu().get(0));
				}

			} else {
				if (room.getLastFaPaiUserId().equals(userId) && room.getGuoUserIds().size() == 0) {
					// 自己摸到牌点过
					List<Integer> nextAction = new ArrayList<Integer>();
					nextAction.add(501);
					room.setNextAction(nextAction);
					room.setNextActionUserId(userId);
				} else {
					// 点击过的人的集合
					room.getGuoUserIds().add(currentPlayer.getUserId());
					if(room.getNextAction().contains(500)){
						//地胡或者有胡杠过
						room.getCanWinPlayerIds().remove(userId);
					}
					// 最后一个人点击过,数量为4.这个牌没人需要.开始发下一张牌. 并清空点过的集合
					if (room.getGuoUserIds().size() == room.getPlayerNum()) {
						room.getGuoUserIds().clear();

						getNextFaPai(room);
					} else {
						MahjongUtils.getNextAction(players, room, room.getLastChuPai());
					}
				}
			}

			// 胡
		} else if (action == 500) {
			logger.info("当前用户选择胡了!!!");
			// TODO 是否杠开
			room.getWinPlayerIds().add(userId);
			currentPlayer.setIsHu(true);
			currentPlayer.setHuNum(currentPlayer.getHuNum() == null ? 1 : currentPlayer.getHuNum() + 1);
			if (room.getIsQiangGangHu() != null && room.getIsQiangGangHu() == true) {
				// 移除最后一个玩家的杠
				ac = new Action(Cnst.ACTION_TYPE_HU, action, userId, room.getLastActionUserId(), null);
				currentMjList.add(room.getLastAction() - 90);
				for (Player p : players) {
					if (p.getUserId().equals(room.getLastActionUserId())) {
						p.setIsDian(true);
						// p.setGangNum(p.getGangNum() == null ? 0 :
						// p.getGangNum()-1);//没有统计杠的次数
						p.setDianNum(p.getDianNum() == null ? 1 : p.getDianNum() + 1);
						for (int i = 0; i < p.getActionList().size(); i++) {
							if (p.getActionList().get(i).getActionId() == room.getLastAction()) {
								p.getActionList().get(i).setActionId(room.getLastAction() - 34);
								p.getActionList().get(i).setType(Cnst.ACTION_TYPE_PENG);
							}
						}
						RedisUtil.updateRedisData(null, p);
						break;
					}
				}
				room.getQiangGangHu().remove(userId);
				if(room.getQiangGangHu().size() == 0){
					// 进入小结算 统计玩家数据
					room.setState(Cnst.ROOM_STATE_XJS);
				}else{
					List<Integer> nextAction = new ArrayList<Integer>();
					nextAction.add(500);
					room.setNextAction(nextAction);
					room.setNextActionUserId(room.getQiangGangHu().get(0));
				}
			} else {
				// 先保留胡的最后一张牌
				if (room.getLastFaPaiUserId().equals(userId) && room.getGuoUserIds().size() == 0) {
					// 自摸
					ac = new Action(Cnst.ACTION_TYPE_HU, action, userId, userId, null);
					currentPlayer.setIsZiMo(true);
					currentPlayer.setZimoNum(currentPlayer.getZimoNum() == null ? 1 : currentPlayer.getZimoNum() + 1);
					// 进入小结算 统计玩家数据
					room.setState(Cnst.ROOM_STATE_XJS);
				} else {
					room.getCanWinPlayerIds().remove(userId);
					ac = new Action(Cnst.ACTION_TYPE_HU, action, userId, room.getLastChuPaiUserId(), null);
					currentMjList.add(room.getLastChuPai());
					for (Player p : players) {
						if (p.getUserId().equals(room.getLastChuPaiUserId())) {
							p.setIsDian(true);
							p.setDianNum(p.getDianNum() == null ? 1 : p.getDianNum() + 1);
							RedisUtil.updateRedisData(null, p);
							break;
						}
					}		
					if(room.getCanWinPlayerIds().size() == 0){
						// 进入小结算 统计玩家数据
						room.setState(Cnst.ROOM_STATE_XJS);
					}else{
						room.setNextActionUserId(room.getCanWinPlayerIds().get(0));
						List<Integer> na = new ArrayList<>();
						na.add(500);
						if(MahjongUtils.checkIsDiHu(room)){
							//地胡可点过
							na.add(0);	
						}
						room.setNextAction(na);
					}
				}				
			}
		} else if (action <= 34 && action >= 1) {
			logger.info("1-34中的牌    出牌!!!");
			ac = new Action(Cnst.ACTION_TYPE_CHUPAI, action, userId, null, null);
			if (currentPlayer.getTing() != null && currentPlayer.getTing().equals(Cnst.TING_STATE_1)) {
				int[] pais = MahjongUtils.getCheckHuPai(currentMjList, null);
				pais[action - 1]--;// 减掉出的这张牌
				pais[33] = 1;// 加入混
				if (Hulib.getInstance().get_hu_info(pais, 34, 33) || MahjongUtils.checkHuTingQiDui(pais, true	, currentPlayer, Cnst.CHECK_PUTONGTING)) {
					currentPlayer.setTing(Cnst.TING_STATE_2);
					room.getGuoUserIds().clear();
				} else {
					logger.info("前端判断听牌有问题");
					illegalRequest(interfaceId, channel);
					return;
				}
			}
			//闲家只要出国牌 就不能算地胡了
			if(!userId.equals(room.getZhuangId())){
				room.setDiHu(false);
			}
			// 设置最后出牌的玩家
			room.setLastChuPaiUserId(userId);
			room.setLastChuPai(action);
			room.setXiaoShuiDi(userId);
			// 移除手牌 添加已出过的牌
			MahjongUtils.removePai(currentPlayer, action);
			currentPlayer.getChuList().add(action);
			// 遍历所有玩家的动作集合.设定 过list
			room.getGuoUserIds().add(currentPlayer.getUserId());
			room.getCanWinPlayerIds().clear();//清空可以胡的玩家集合
			// 检测所有玩家的动作并排序.
			MahjongUtils.getNextAction(players, room, action);
			if(moBao(room,currentPlayer,action)){
				
			}else{
				huanBao:for(Player p:players){
					if(p.getTing()!=null && p.getTing() == Cnst.TING_STATE_2){
						//这里要执行换宝逻辑
						if(huanBao(room,action,players) == Cnst.ZHENGCHANG_HUANBAO){
							huanBao = true;
							break huanBao;
						}
					}
				}
			}
			logger.info("当前的宝牌"+room.getBaoPai());
			// 吃
		} else if ((action >= 175 && action <= 196)) { // 吃
			if (action >= 175 && action <= 196) {
				logger.info("175-196中的牌    我吃听了!!!");
				currentPlayer.setTing(Cnst.TING_STATE_1);// 刚点听，没出牌
			} else {
				logger.info("35-56中的牌    我吃了!!!");
			}
			room.setXiaoShuiDi(null);
			room.setWindPosition(userId);
			Integer realChiAction = action > 174 ? action - 140 : action;
			List<Integer> chi = MahjongUtils.reChiList(realChiAction, room.getLastChuPai());// 吃完组成的集合
			// 玩家 动作集合
			ac = new Action(Cnst.ACTION_TYPE_CHI, action, currentPlayer.getUserId(), room.getLastChuPaiUserId(), room.getLastChuPai());
			currentPlayer.addActionList(ac);
			// 移除手牌
			MahjongUtils.removeActionMj(currentMjList, chi, realChiAction, Cnst.ACTION_TYPE_CHI);
			// 移除桌面上被玩家吃掉的牌
			MahjongUtils.removeCPG(room, players);
			// 吃完是请求出牌
			List<Integer> actionList = new ArrayList<Integer>();
			actionList.add(501);
			room.setNextAction(actionList);
			room.setNextActionUserId(userId);
			// 碰
		} else if ((action >= 57 && action <= 90) || (action >= 197 && action <= 230)) {
			if (action >= 197 && action <= 230) {
				logger.info("197-230中的牌    我碰听了啊!!!");
				currentPlayer.setTing(Cnst.TING_STATE_1);
			} else {
				logger.info("57-90中的牌    我碰了啊!!!");
			}
			room.setLastFaPaiUserId(userId);
			room.setXiaoShuiDi(null);
			room.setWindPosition(userId);
			// 玩家 动作集合
			// Integer type, Integer actionId, Long userId, Long toUserId,
			// Integer extra

			Integer realPengAction = action > 174 ? action - 140 : action;
			ac = new Action(Cnst.ACTION_TYPE_PENG, action, currentPlayer.getUserId(), room.getLastChuPaiUserId(), room.getLastChuPai());
			currentPlayer.addActionList(ac);
			// 移除手牌
			MahjongUtils.removeActionMj(currentMjList, null, realPengAction, Cnst.ACTION_TYPE_PENG);
			// 移除桌面上被玩家吃掉的牌
			MahjongUtils.removeCPG(room, players);

			// 碰完是请求出牌
			List<Integer> actionList = new ArrayList<Integer>();
			if (action >= 57 && action <= 90 && MahjongUtils.checkTing(currentPlayer, room,Cnst.CHECK_PENGTING)) {
				actionList.add(Cnst.ACTION_BIANMA_TING);
				actionList.add(Cnst.ACTION_BIANMA_BAOLOU);
				actionList.add(0);
			} else {
				actionList.add(501);
			}
			room.setNextAction(actionList);
			room.setNextActionUserId(userId);
			// 杠
		} else if (action >= 91 && action <= 126) {
			logger.info("91-126中的牌    我杠了啊!!!");
			room.setWindPosition(userId);
			Integer pai = action - 90;// 杠的牌
			// 这里需要检测是碰杠 点杠 或 暗杠
			Iterator<Integer> it = currentMjList.iterator();
			Integer gangPaiNum = 0;
			while (it.hasNext()) {
				Integer x = it.next();
				if (x == pai) {
					gangPaiNum++;
				}
			}

			if (gangPaiNum == 1) {
				// 碰杠 先遍历用户动作集合 移除碰的集合 加上碰杠操作

				List<Action> actionList = currentPlayer.getActionList();
				Long toUserId = null;
				for (Action a : actionList) {
					if (a.getType() == Cnst.ACTION_TYPE_PENG && a.getExtra() == pai) {
						toUserId = a.getToUserId();
						actionList.remove(a);
						break;
					}
				}
				ac = new Action(Cnst.ACTION_TYPE_PENGGANG, action, currentPlayer.getUserId(), toUserId, pai);
				actionList.add(ac);
				currentPlayer.setActionList(actionList);
				MahjongUtils.removeActionMj(currentMjList, null, action, Cnst.ACTION_TYPE_PENGGANG);
				// 移除桌面上被玩家吃掉的牌
				MahjongUtils.removeCPG(room, players);

				for (Player p : players) {
					if (p.getUserId().equals(userId)) {
						continue;
					}
					List<Integer> qiangGang = MahjongUtils.checkActionList(p, pai, room, Cnst.CHECK_TYPE_QIANGGANG, false);
					if (qiangGang.size() > 0) {
						room.getQiangGangHu().add(p.getUserId());
						room.setNextAction(qiangGang);
					}
				}
				if (room.getQiangGangHu().size() > 0) {
					room.setIsQiangGangHu(true);
					room.setNextActionUserId(room.getQiangGangHu().get(0));
				}

			} else if (gangPaiNum == 4) {
				// 暗杠
				ac = new Action(Cnst.ACTION_TYPE_ANGANG, action, userId, null, pai);
				currentPlayer.addActionList(ac);
				// 移除手牌
				MahjongUtils.removeActionMj(currentMjList, null, action, Cnst.ACTION_TYPE_ANGANG);
			} else if (gangPaiNum == 3) {
				// 点杠
				room.setXiaoShuiDi(null);
				ac = new Action(Cnst.ACTION_TYPE_DIANGANG, action, userId, room.getLastChuPaiUserId(), pai);
				currentPlayer.addActionList(ac);
				// 移除手牌
				MahjongUtils.removeActionMj(currentMjList, null, action, Cnst.ACTION_TYPE_DIANGANG);
				// 移除桌面上被玩家吃掉的牌
				MahjongUtils.removeCPG(room, players);
			}

			// 杠完摸牌
			if(room.getIsQiangGangHu() != null && room.getIsQiangGangHu() == true){
				//胡牌
			}else{
				//杠完摸牌
				List<Integer> nextAction = new ArrayList<Integer>();
				nextAction.add(-1);
				room.setNextAction(nextAction);
				room.setNextActionUserId(userId);
			}
		} else if (action == Cnst.ACTION_BIANMA_TING) {// 听
			logger.info("玩家点听牌!!!");
			ac = new Action(Cnst.ACTION_TYPE_TING, action, userId, null, null);
			currentPlayer.setTing(Cnst.TING_STATE_1);
			List<Integer> currentPlayerActionList = new ArrayList<Integer>();
			currentPlayerActionList.add(501);
			room.setNextAction(currentPlayerActionList);
			room.setNextActionUserId(userId);
		} else if (action == Cnst.ACTION_BIANMA_BAOLOU){
			//暴漏
			logger.info("玩家点暴漏!!!");
			ac = new Action(Cnst.ACTION_TYPE_BAOLOU, action, userId, null, null);
			currentPlayer.setBaoLou(true);
			List<Integer> currentPlayerActionList = new ArrayList<Integer>();
			for(Integer i:room.getNextAction()){
				if(i>=174 && i<=230){
					currentPlayerActionList.add(i);
				}
			}
			room.setNextAction(currentPlayerActionList);
			room.setNextActionUserId(userId);
		} else if(action == Cnst.ACTION_BIANMA_BAOTING){
			logger.info("玩家点报听!!!");
			ac = new Action(Cnst.ACTION_TYPE_BAOTING, action, userId, null, null);
			List<Integer> currentPlayerActionList = new ArrayList<Integer>();
			for(Integer i:room.getNextAction()){
				if(i>=174 && i<=230){
					currentPlayerActionList.add(i);
				}
			}
			room.setNextAction(currentPlayerActionList);
			room.setNextActionUserId(userId);
			// TODO 更新Redis
			RedisUtil.updateRedisData(room, currentPlayer);
			// 写入回放
			BackFileUtil.save(interfaceId, room, players, null, ac);
			return;
		}

		// TODO 更新Redis
		RedisUtil.updateRedisData(room, currentPlayer);
		// 写入回放
		BackFileUtil.save(interfaceId, room, players, null, ac);
		// 小结算
		if (room.getState() == Cnst.ROOM_STATE_XJS) {
			JieSuan.xiaoJieSuan(String.valueOf(roomId));
		}
		// 统一发消息即可 跟据action 来判断
		MessageFunctions.interface_100104(room, players, 100104, ac,huanBao);
	}

	/**
	 * 通过下标获取到下一个要发牌的人. 海底牌
	 * 
	 * @param room
	 */
	private static void getNextFaPaiHaiDi(RoomResp room, Long userId) {
		List<Integer> nextAction = new ArrayList<Integer>();
		nextAction.add(-1);
		room.setNextAction(nextAction);
		// 取到上个出牌人的角标 下一位来发牌
		int index = -1;
		Long[] playIds = room.getPlayerIds();
		for (int i = 0; i < playIds.length; i++) {
			if (playIds[i].equals(userId)) {
				index = i + 1;
				if (index == playIds.length) {
					index = 0;
				}
				break;
			}
		}
		room.setNextActionUserId(playIds[index]);
	}

	/**
	 * 通过下标获取到下一个要发牌的人. 正常 发牌
	 * 
	 * @param room
	 */
	private static void getNextFaPai(RoomResp room) {
		List<Integer> nextAction = new ArrayList<Integer>();
		nextAction.add(-1);
		room.setNextAction(nextAction);
		// 取到上个出牌人的角标 下一位来发牌
		int index = -1;
		Long[] playIds = room.getPlayerIds();
		for (int i = 0; i < playIds.length; i++) {
			if (playIds[i].equals(room.getLastChuPaiUserId())) {
				index = i + 1;
				if (index == playIds.length) {
					index = 0;
				}
				break;
			}
		}
		room.setNextActionUserId(playIds[index]);
	}

	/**
	 * 玩家申请解散房间
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	public synchronized static void interface_100203(WSClient channel, Map<String, Object> readData) throws Exception {
		logger.info("玩家请求解散房间,interfaceId -> 100203");
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomId"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId));
		if (room.getDissolveRoom() != null) {
			return;
		}
		DissolveRoom dis = new DissolveRoom();
		dis.setDissolveTime(new Date().getTime());
		dis.setUserId(userId);
		List<Map<String, Object>> othersAgree = new ArrayList<>();
		List<Player> players = RedisUtil.getPlayerList(room);
		for (Player p : players) {
			if (!p.getUserId().equals(userId)) {
				Map<String, Object> map = new HashMap<>();
				map.put("userId", p.getUserId());
				map.put("agree", 0);// 1同意；2解散；0等待
				othersAgree.add(map);
			}
		}
		dis.setOthersAgree(othersAgree);
		room.setDissolveRoom(dis);

		Map<String, Object> info = new HashMap<>();
		info.put("dissolveTime", dis.getDissolveTime());
		info.put("userId", dis.getUserId());
		info.put("othersAgree", dis.getOthersAgree());
		JSONObject result = getJSONObj(interfaceId, 1, info);
		for (Player p : players) {
			WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
			if (ws != null) {
				MessageUtils.sendMessage(ws, result.toJSONString());
			}
		}

		for (Player p : players) {
			RedisUtil.updateRedisData(null, p);
		}
		RedisUtil.updateRedisData(room, null);

		// 解散房间超时任务开启 TODO
		RoomUtil.addFreeRoomTask(StringUtils.parseLong(room.getRoomId()), System.currentTimeMillis() + Cnst.ROOM_DIS_TIME);
	}

	/**
	 * 同意或者拒绝解散房间
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */

	public synchronized static void interface_100204(WSClient channel, Map<String, Object> readData) throws Exception {
		logger.info("同意或者拒绝解散房间,interfaceId -> interface_100204");
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomId"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		Integer userAgree = StringUtils.parseInt(readData.get("userAgree"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId));
		if (room == null) {// 房间已经自动解散
			Map<String, Object> info = new HashMap<>();
			info.put("reqState", Cnst.REQ_STATE_4);
			JSONObject result = getJSONObj(interfaceId, 1, info);
			MessageUtils.sendMessage(channel, result.toJSONString());
			return;
		}
		if (room.getDissolveRoom() == null) {
			Map<String, Object> info = new HashMap<>();
			info.put("reqState", Cnst.REQ_STATE_7);
			JSONObject result = getJSONObj(interfaceId, 1, info);
			MessageUtils.sendMessage(channel, result.toJSONString());
			return;
		}
		List<Map<String, Object>> othersAgree = room.getDissolveRoom().getOthersAgree();
		for (Map<String, Object> m : othersAgree) {
			if (String.valueOf(m.get("userId")).equals(String.valueOf(userId))) {
				m.put("agree", userAgree);
				break;
			}
		}
		Map<String, Object> info = new HashMap<>();
		info.put("dissolveTime", room.getDissolveRoom().getDissolveTime());
		info.put("userId", room.getDissolveRoom().getUserId());
		info.put("othersAgree", room.getDissolveRoom().getOthersAgree());
		JSONObject result = getJSONObj(interfaceId, 1, info);

		int needAgreeNum = room.getPlayerNum() == 4 ? 2 : 1;
		int needRejectNum = room.getPlayerNum() == 2 ? 1 : 2;

		int agreeNum = 0;
		int rejectNum = 0;
		for (Map<String, Object> m : othersAgree) {
			if (m.get("agree").equals(1)) {
				agreeNum++;
			} else if (m.get("agree").equals(2)) {
				rejectNum++;
			}
		}
		if (rejectNum >= needRejectNum) {
			// 有玩家拒绝解散房间//关闭解散房间计时任务 TODO
			RoomUtil.removeFreeRoomTask(StringUtils.parseLong(room.getRoomId()));
			room.setDissolveRoom(null);
			RedisUtil.setObject(Cnst.REDIS_PREFIX_ROOMMAP.concat(String.valueOf(roomId)), room, Cnst.ROOM_LIFE_TIME_CREAT);
		}
		RedisUtil.updateRedisData(room, null);
		List<Player> players = RedisUtil.getPlayerList(room);

		if (agreeNum >= needAgreeNum) {
			// 解散房间是 xiaoJSInfo 写入0

			if (room.getRoomType() == Cnst.ROOM_TYPE_2) {
				MessageFunctions.interface_100112(null, room, Cnst.PLAYER_EXTRATYPE_JIESANROOM);
			}
			RoomUtil.updateDatabasePlayRecord(room);

			room.setState(Cnst.ROOM_STATE_YJS);

			for (Player p : players) {
				// TODO
				p.initPlayer(null, Cnst.PLAYER_STATE_DATING, null);
			}
			room.setDissolveRoom(null);
			RedisUtil.setObject(Cnst.REDIS_PREFIX_ROOMMAP.concat(String.valueOf(roomId)), room, Cnst.ROOM_LIFE_TIME_DIS);
			RedisUtil.setPlayersList(players);
			// 关闭解散房间计时任务 TODO
			RoomUtil.removeFreeRoomTask(StringUtils.parseLong(room.getRoomId()));
		}

		for (Player p : players) {
			WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
			if (ws != null) {
				MessageUtils.sendMessage(ws, result.toJSONString());
			}
		}

	}

	/**
	 * 退出房间
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	public synchronized static void interface_100205(WSClient channel, Map<String, Object> readData) throws Exception {
		logger.info("退出房间,interfaceId -> 100205");
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomId"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId));
		if (room == null) {
			roomDoesNotExist(interfaceId, channel);
			return;
		}
		if (room.getState() == Cnst.ROOM_STATE_CREATED) {
			List<Player> players = RedisUtil.getPlayerList(room);
			Map<String, Object> info = new HashMap<>();
			info.put("userId", userId);
			if (room.getCreateId().equals(userId)) {// 房主退出，
				if (room.getRoomType().equals(Cnst.ROOM_TYPE_1)) {// 房主模式
					int circle = room.getCircleNum();
					info.put("type", Cnst.EXIST_TYPE_DISSOLVE);
					if (String.valueOf(roomId).length() == 6) {
						for (Player p : players) {
							if (p.getUserId().equals(userId)) {
								p.setMoney(p.getMoney() + Cnst.moneyMap.get(circle));
								break;
							}
						}
					} else if (String.valueOf(roomId).length() == 7) {
						// 退还俱乐部房卡
						ClubInfo clubInfo = RedisUtil.getClubInfoByClubId(room.getClubId().toString());
						clubInfo.setRoomCardNum(clubInfo.getRoomCardNum() + Cnst.getClubRoomNeedMoney(room.getPlayerNum(), room.getCircleNum()));
						RedisUtil.setClubInfoByClubId(String.valueOf(room.getClubId()), clubInfo);
						// 移除俱乐部创建房间缓存
						RedisUtil.hdel(Cnst.REDIS_CLUB_ROOM_LIST.concat(String.valueOf(room.getClubId())), String.valueOf(room.getRoomId()));
					}

					RedisUtil.deleteByKey(Cnst.REDIS_PREFIX_ROOMMAP.concat(String.valueOf(roomId)));

					for (Player p : players) {
						// TODO
						p.initPlayer(null, Cnst.PLAYER_STATE_DATING, null);
					}
					// 关闭解散房间计时任务 TODO
					RoomUtil.removeFreeRoomTask(StringUtils.parseLong(room.getRoomId()));
				} else {// 自由模式，走正常退出
					info.put("type", Cnst.EXIST_TYPE_EXIST);
					existRoom(room, players, userId);
					RedisUtil.updateRedisData(room, null);
				}
			} else {// 正常退出
				for (Player player : players) {
					if (player.getUserId().equals(userId)) {// 找到退出的玩家
						// 如果加入的代开房间 通知房主
						if (room.getRoomType() == Cnst.ROOM_TYPE_2 && !userId.equals(room.getCreateId())) {
							MessageFunctions.interface_100112(player, room, Cnst.PLAYER_EXTRATYPE_EXITROOM);
						}
					}
				}
				info.put("type", Cnst.EXIST_TYPE_EXIST);
				existRoom(room, players, userId);
				RedisUtil.updateRedisData(room, null);
			}
			JSONObject result = getJSONObj(interfaceId, 1, info);
			for (Player p : players) {
				RedisUtil.updateRedisData(null, p);
			}

			for (Player p : players) {
				WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
				if (ws != null) {
					MessageUtils.sendMessage(ws, result.toJSONString());
				}
			}
		} else {
			roomIsGaming(interfaceId, channel);
		}
	}

	private static void existRoom(RoomResp room, List<Player> players, Long userId) {
		for (Player p : players) {
			if (p.getUserId().equals(userId)) {
				// TODO
				p.initPlayer(null, Cnst.PLAYER_STATE_DATING, null);
				break;
			}
		}
		Long[] pids = room.getPlayerIds();
		if (pids != null) {
			for (int i = 0; i < pids.length; i++) {
				if (userId.equals(pids[i])) {
					pids[i] = null;
					break;
				}
			}
		}
	}

	/**
	 * 语音表情
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	public static void interface_100206(WSClient channel, Map<String, Object> readData) throws Exception {
		logger.info("语音表情,interfaceId -> 100206");
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomId"));
		String userId = String.valueOf(readData.get("userId"));
		String type = String.valueOf(readData.get("type"));
		String idx = String.valueOf(readData.get("idx"));
		Map<String, Object> info = new HashMap<>();
		info.put("roomId", roomId);
		info.put("userId", userId);
		info.put("type", type);
		info.put("idx", idx);
		JSONObject result = getJSONObj(interfaceId, 1, info);
		List<Player> players = RedisUtil.getPlayerList(roomId);
		for (Player p : players) {
			if (!p.getUserId().equals(userId)) {
				WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
				if (ws != null) {
					MessageUtils.sendMessage(ws, result.toJSONString());
				}
			}
		}
	}

	/**
	 * 定位
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	public static void interface_100207(WSClient channel, Map<String, Object> readData) throws Exception {
		logger.info("定位,interfaceId -> 100207");
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomId"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId));
		if (room == null) {
			return;
		}
		List<Player> players = RedisUtil.getPlayerList(room);
		List<Map<String, Object>> info = new ArrayList<Map<String, Object>>();
		List<Player> agreePlayers = new ArrayList<Player>();
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).getX_index() != null && players.get(i).getY_index() != null) {
				agreePlayers.add(players.get(i));
			}
		}
		if (agreePlayers.size() > 1) {
			for (int i = 0; i < agreePlayers.size(); i++) {
				for (int m = i + 1; m < agreePlayers.size(); m++) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("userId", agreePlayers.get(i).getUserId());
					map.put("toUserId", agreePlayers.get(m).getUserId());
					Double x1 = agreePlayers.get(i).getX_index() - agreePlayers.get(m).getX_index();
					Double y1 = agreePlayers.get(i).getY_index() - agreePlayers.get(m).getY_index();
					map.put("distance", (int) Math.floor(Math.sqrt(x1 * x1 + y1 * y1)));
					info.add(map);
				}
			}
		}
		JSONObject result = getJSONObj(interfaceId, 1, info);
		MessageUtils.sendMessage(channel, result.toJSONString());
	}

	/**
	 * 牌堆里没有这张牌 要进行换宝
	 * @param room
	 * @param pai
	 * @return
	 */
	public static int huanBao(RoomResp room,Integer pai,List<Player> players){
		if(room.getBaoPai() == 0){
			//真正意义的定宝
			room.setBaoPai(room.getFirstBaoPai());
			return Cnst.ZHENGCHANG_HUANBAO;
		}
//		if(!pai.equals(room.getBaoPai())){
//			//不需要换宝
//			return Cnst.BU_HUANBAO;
//		}
		List<Integer> roomMJ = MahjongUtils.getNewList(room.getCurrentMjList());
		//添加手牌
		for(Player p:players){
			roomMJ.addAll(p.getCurrentMjList());
		}
		int num = 0;
		for(int i=0;i<roomMJ.size();i++){
			if(roomMJ.get(i).equals(room.getBaoPai())){
				num++;
			}
		}
		//暗着的小于等于1张 这时需要换宝
		if(num<=1){
			room.setBaoPai((int)(Math.random()*27)+1);
			return Cnst.ZHENGCHANG_HUANBAO;
		}
		return Cnst.BU_HUANBAO;
	}
	
	/**
	 * 通宝逻辑
	 * @param room
	 * @param p
	 * @param pai
	 * @return
	 */
	public static boolean moBao(RoomResp room,Player p,Integer pai){
		if(p.getTing() != Cnst.TING_STATE_2){
			return false;
		}
		//判断别人是否可以胡 不能算摸宝
		if(room.getNextAction().contains(500)){
			return false;
		}
		if(room.getBaoPai() == 0){
			//还没设置过宝牌
			room.setBaoPai(room.getFirstBaoPai());
		}
		//在验证一下牌堆里是否有这张宝牌
//		if(!room.getCurrentMjList().contains(room.getBaoPai())){
//			return false;
//		}
		int[] pais = MahjongUtils.getCheckHuPai(p.getCurrentMjList(), null);
		pais[room.getBaoPai()-1]++;//加一个宝牌是否可以胡
		if (Hulib.getInstance().get_hu_info(pais, 34, 34) || MahjongUtils.checkHuTingQiDui(pais, false, p, Cnst.CHECK_PUTONGTING)) {
			//这里可以通宝
			List<Integer> currentAction = new ArrayList<Integer>();
			currentAction.add(500);
			room.setNextAction(currentAction);
			room.setNextActionUserId(p.getUserId());
			room.setLastFaPaiUserId(p.getUserId());
			room.getGuoUserIds().clear();
			room.getCanWinPlayerIds().clear();
			room.getCanWinPlayerIds().add(p.getUserId());
			p.setMoBao(true);
			p.getCurrentMjList().add(room.getBaoPai());
		} else {
			return false ;
		}
		return true;
	}
}
