package com.yzt.logic.util.GameUtil;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.yzt.logic.mj.domain.Action;
import com.yzt.logic.mj.domain.Player;
import com.yzt.logic.mj.domain.RoomResp;
import com.yzt.logic.util.BackFileUtil;
import com.yzt.logic.util.Cnst;
import com.yzt.logic.util.MahjongUtils;
import com.yzt.logic.util.RoomUtil;
import com.yzt.logic.util.redis.RedisUtil;

/**
 * 玩家分的统计
 * 
 * @author wsw_007
 *
 */
public class JieSuan {
	
	public static void xiaoJieSuan(String roomId) {
		RoomResp room = RedisUtil.getRoomRespByRoomId(roomId);
		List<Player> players = RedisUtil.getPlayerList(room);
		//需要做以下统计
		//以及大结算校验  这里会写小结算文件 并对房间进行初始化 
		boolean ziMo = false;//赢家是否自摸
		for (Player other : players) {
			if(other.getIsZiMo()){
				ziMo = true;
				break;
			}
		}
		//杠分单算,先取到每个玩家的杠分.
		for (Player player : players) {
			List<Action> actionList = player.getActionList();
			if (actionList != null && actionList.size() != 0) {
				for (Action action : actionList) {
					if (action.getType() == Cnst.ACTION_TYPE_DIANGANG || action.getType() == Cnst.ACTION_TYPE_PENGGANG) { // 明杠1分
						changeGangFen(action,players,player, room,Cnst.MINGGANG);
					} else if (action.getType() == Cnst.ACTION_TYPE_ANGANG) { // 暗杠2分
						changeGangFen(action,players,player, room,Cnst.ANGANG);
					}
				}
			}
		}


		//FIXME
		//统计玩家各项数据 庄次数 胡的次数 特殊胡的次数 自摸次数 点炮次数 胡牌类型 具体番数 各个分数统计 
		if(room.getHuangZhuang() != null && room.getHuangZhuang() == true){
//			//荒庄不荒杠
//			for(Player p : players){
//				p.setScore(p.getScore()+p.getGangScore());
//			}
		}else{ //正常结算
			MahjongUtils.checkHuFenInfo(players,room); // 检查胡牌玩家的分数	
			
			// 计分方式：点炮包三家，听牌点炮三家付			
			if(room.getWinPlayerIds().contains(room.getZhuangId())){
				//庄不变
			}else{
				//下个人坐庄
				int index = -1;
				Long[] playIds = room.getPlayerIds();
				for(int i=0;i<playIds.length;i++){
					if(playIds[i].equals(room.getZhuangId())){
						index = i+1;
						if(index == playIds.length){
							index = 0;
						}
						break;
					}
				}
				room.setZhuangId(playIds[index]);
				room.setCircleWind(index+1);
				
				//不是第一局,并且圈风是东风 ,证明是下一圈了.
				if(room.getXiaoJuNum() != 1 && room.getCircleWind() == Cnst.WIND_EAST){
					room.setTotolCircleNum(room.getTotolCircleNum() == null ? 1:room.getTotolCircleNum()+1);
					room.setLastNum(room.getCircleNum() - room.getTotolCircleNum());
				}
			}
		}
		
	
		// 更新redis
		RedisUtil.setPlayersList(players);
		
		// 添加小结算信息
		List<Integer> xiaoJS = new ArrayList<Integer>();
		for (Player p : players) {
			xiaoJS.add(p.getThisScore()+p.getGangScore());
		}
		room.addXiaoJuInfo(xiaoJS);
		// 初始化房间
		room.initRoom();
		RedisUtil.updateRedisData(room, null);
		// 写入文件
		List<Map<String, Object>> userInfos = new ArrayList<Map<String, Object>>();
		for (Player p : players) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("userId", p.getUserId());
			map.put("gangScore", p.getGangScore());
			map.put("huScore", p.getThisScore());
			map.put("pais", p.getCurrentMjList());
			map.put("winInfo", p.getFanShu());
			if(p.getIsHu()){
				map.put("isWin", 1);
			}else{
				map.put("isWin", 0);
			}
			if(p.getIsDian()){
				map.put("isDian", 1);
			}else{
				map.put("isDian", 0);
			}
			if(p.getActionList() != null && p.getActionList().size() > 0){
				List<Object> actionList = new ArrayList<Object>();
				for(Action action : p.getActionList()){
					if(action.getType() == Cnst.ACTION_TYPE_CHI){
						Map<String,Integer> actionMap = new HashMap<String, Integer>();
						actionMap.put("action", action.getActionId());
						actionMap.put("extra", action.getExtra());
						actionList.add(actionMap);
						
					}else if(action.getType() == Cnst.ACTION_TYPE_ANGANG){
						Map<String,Integer> actionMap = new HashMap<String, Integer>();
						actionMap.put("action", -2);
						actionMap.put("extra", action.getActionId());
						actionList.add(actionMap);
					}else{
						actionList.add(action.getActionId());
					}
				}
				map.put("actionList", actionList);
			}			
			userInfos.add(map);
		}
		JSONObject info = new JSONObject();
		info.put("baoPai", room.getBaoPai() == 0?null:room.getBaoPai());
		info.put("lastNum", room.getLastNum());
		info.put("userInfo", userInfos);
		BackFileUtil.save(100102, room, null, info,null);
		// 小结算 存入一次回放
		BackFileUtil.write(room);

		// 大结算判定 (玩的圈数等于选择的圈数)
		if (room.getTotolCircleNum() == room.getCircleNum()) {
			// 最后一局 大结算
			room = RedisUtil.getRoomRespByRoomId(roomId);
			room.setState(Cnst.ROOM_STATE_YJS);
			RedisUtil.updateRedisData(room, null);
			// 这里更新数据库吧
			RoomUtil.updateDatabasePlayRecord(room);
		}
	}

	private static void changeGangFen(Action action, List<Player> players,
			Player player, RoomResp room, int type) {
		// TODO Auto-generated method stub
		if(type == Cnst.MINGGANG){
			player.setGangScore(player.getGangScore()+1);
			for(Player p:players){
				if(p.getUserId().equals(action.getToUserId())){
					p.setGangScore(p.getGangScore()-1);
					break;
				}
			}
		}else{
			player.setGangScore(player.getGangScore()+room.getPlayerNum()-1);
			for(Player p:players){
				if(p.getUserId().equals(player.getUserId())){
					continue;
				}
				p.setGangScore(p.getGangScore() - 1);
			}
		}
	}
	
	
}
