package com.yzt.logic.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.yzt.logic.mj.domain.Action;
import com.yzt.logic.mj.domain.Player;
import com.yzt.logic.mj.domain.RoomResp;
import com.yzt.logic.util.JudegHu.checkHu.Hulib;
import com.yzt.logic.util.JudegHu.checkHu.TableMgr;
import com.yzt.logic.util.redis.RedisUtil;

/**
 * 
 * @author wsw_007
 *
 */
public class MahjongUtils {

	static {
		// 加载胡的可能
		TableMgr.getInstance().load();
	}
	
	
	/**
	 * 
	 * @return
	 */
	public static List<Integer> getPais(RoomResp room) {
		// 1-9万 ,10-18饼,19-27条,32红中.
		ArrayList<Integer> pais = new ArrayList<Integer>();
		for (int j = 0; j < 4; j++) {
			for (int i = 1; i <= 27; i++) {
				pais.add(i);
			}
			pais.add(32);
		}
		// 2.洗牌
		Collections.shuffle(pais);
		return pais;
		
	}

	
	/**
	 * 删除用户指定的一张牌
	 * 
	 * @param currentPlayer
	 * @return
	 */
	public static void removePai(Player currentPlayer, Integer action) {
		Iterator<Integer> pai = currentPlayer.getCurrentMjList().iterator();
		while (pai.hasNext()) {
			Integer item = pai.next();
			if (item.equals(action)) {
				pai.remove();
				break;
			}
		}
	}

	
	/**
	 * 
	 * @param room
	 *            房间
	 * @param currentPlayer
	 *            当前操作的玩家
	 * @return 返回需要通知的操作的玩家ID
	 */
	public static Long nextActionUserId(RoomResp room, Long lastUserId) {
		Long[] playerIds = room.getPlayerIds();

		for (int i = 0; i < playerIds.length; i++) {
			if (lastUserId == playerIds[i]) {
				if (i == playerIds.length - 1) { // 如果是最后 一个,则取第一个.
					return playerIds[0];
				} else {
					return playerIds[i + 1];
				}
			}
		}
		return -100l;
	}

 

	/**
	 *
	 * @return true 为可以胡牌牌型
	 */

	public static boolean checkHuRule(Player p,RoomResp room ,Integer pai,Integer type) {
		List<Integer> newList = getNewList(p.getCurrentMjList());
		
		if (type.equals(Cnst.CHECK_TYPE_ZIJIMO)||type.equals(Cnst.CHECK_TYPE_HAIDIANPAI)||type.equals(Cnst.CHECK_TYPE_TIANHU)) {
			if(p.getTing() == Cnst.TING_STATE_2 && pai == room.getBaoPai()){
				return true;
			}
			int[] pais = getCheckHuPai(newList, null);
			if (Hulib.getInstance().get_hu_info(pais, 34, 34) || checkHuTingQiDui(pais, false,p,Cnst.CHECK_PUTONGTING)) {
				return true;
			}
		}else{
			if(p.isBaoLou()){
				return false;
			}
			newList.add(pai); //检测要带上别人打出的牌
			int[] pais = getCheckHuPai(newList, null);
			if (Hulib.getInstance().get_hu_info(pais, 34, 34) || checkHuTingQiDui(pais, false,p,Cnst.CHECK_PUTONGTING)) {
				return true;
			}
		}
		
			
		return false;		
	}
	
	/**
	 * 
	 * @Title: checkTing   
	 * @Description: 手牌个数一定是3n+2
	 * @param: @param p
	 * @param: @param room
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	public static boolean checkTing(Player p,RoomResp room,Integer type){
		List<Integer> newList = getNewList(p.getCurrentMjList());
		newList.add(34);//加一个混
		int[] pais = getCheckHuPai(newList, null);
		for (int n = 0; n < pais.length; n++) {
			if (pais[n]>0&&n!=33) {  
				pais[n]--;
				if (Hulib.getInstance().get_hu_info(pais, 34, 33) || checkHuTingQiDui(pais, true,p,type)) {
					return true;
				}
				pais[n]++;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @Title: checkTing   
	 * @Description: 手牌个数一定是3n+2
	 * @param: @param p
	 * @param: @param room
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	public static List<Map<String,Object>> checkTingToChuList(Player p,Integer type){
		List<Map<String,Object>> tingToChuList = new ArrayList<Map<String,Object>>();
		List<Integer> newList = getNewList(p.getCurrentMjList());
		newList.add(34);//加一个混   
		int[] pais = getCheckHuPai(newList, null); 
		for (int n = 0; n < pais.length; n++) {
			if (pais[n]>0&&n!=33 ) {
				pais[n]--;
				if (Hulib.getInstance().get_hu_info(pais, 34, 33) || checkHuTingQiDui(pais, true,p,type)) {
					//可以出的牌
					Map<String,Object> map = new HashMap<String, Object>();
					map.put("action", n+1);
					List<Integer> huDePai = new ArrayList<Integer>();
					//混去掉 挨个加
					pais[33] = 0;
					for(int m=0;m<pais.length;m++){
						pais[m]++;
						if(Hulib.getInstance().get_hu_info(pais, 34, 34) || checkHuTingQiDui(pais, true,p,type)){
							huDePai.add(m+1);
						}
						pais[m]--;
					}
					map.put("extra", huDePai);
					tingToChuList.add(map);
					pais[33]++;
				}
				pais[n]++;
			}
		}
		return tingToChuList.size()>0?tingToChuList:null;
	}

	/**
	 * 
	 * @Title: isQingYiSe   
	 * @Description: 重点想要不是清一色的结果
	 * @param: @param p
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	public static boolean isQingYiSe(Player p,List<Integer> pais){
		boolean isQingYiSe = true;
		int type = 0;
		a:for(Integer pai:pais){
			if (pai!=32) {
				type = (pai-1)/9;
				for(Integer temp:pais){
					if (temp!=32&&(temp-1)/9!=type) {
						isQingYiSe = false;
						break a;
					}
				}
			}
		}
		if (isQingYiSe) {
			List<Action> actions = p.getActionList();
			if (actions!=null&&actions.size()>0) {
				for(Action act:actions){
					Integer actId = act.getActionId();
					if ((actId >=35 && actId <=56)||(actId >=175 && actId <=196)) {//吃或者抢吃
						if(actId >=175 && actId <=196){
							actId = actId-140;
						}
						int[] chis = Cnst.chiMap.get(actId);
						if ((chis[0]-1)/9!=type) {
							isQingYiSe = false;
							break;
						}
					}else if((actId >=57 && actId <=90)||(actId >=197 && actId <=230)){
						if(actId >=197 && actId <=230){
							actId = actId-140;
						}
						if ((actId-56-1)/9!=type) {
							isQingYiSe = false;
							break;
						}
					}else if(actId >=91 && actId <=126){
						if ((actId-90-1)/9!=type) {
							isQingYiSe = false;
							break;
						}
					}
					
				}
			}
		}
		return isQingYiSe;
	}
	
	/**
	 * 检测动作集合
	 * @param p
	 * @param pai
	 * @param room
	 * @param type
	 * @param checkChi 自己打的牌true,不提示吃.
	 * @return
	 */
	public static List<Integer> checkActionList(Player p, Integer pai, RoomResp room,Integer type,Boolean checkChi) {
		List<Integer> actionList = new ArrayList<Integer>();
		boolean canTing = false;//增加暴漏按钮
		boolean ting = p.getTing() != 0;
		boolean tingGang = false;//可以听杠
		if (type == Cnst.CHECK_TYPE_ZIJIMO) {//自摸
			if (ting) {
				if (checkHuRule(p,room,pai,type)) {
					actionList.add(500);	
				}
				//自摸的时候,检测能不能碰杠
				if(Cnst.CHECK_TYPE_ZIJIMO == type){
					List<Integer> pengGang = checkPengGang(p, pai);
					if(pengGang.size() != 0 && pengGang.contains(pai+90)){
						actionList.add(pai+90);
						tingGang = true;
					}
					
				}
				//自摸的时候,检测能不能暗杠.//杠完还是听牌结构
				if(isShouBaYi(p, type, Cnst.ACTION_TYPE_ANGANG) && Cnst.CHECK_TYPE_ZIJIMO == type && checkTingGang(p, room, pai, 4)){
					actionList.add(pai+90);
					tingGang = true;
				}
			}else{
				if (checkTing(p, room,Cnst.CHECK_PUTONGTING)) {
					canTing = true;
					actionList.add(Cnst.ACTION_BIANMA_TING);//听
				}
				//自摸的时候,检测能不能碰杠
				if(Cnst.CHECK_TYPE_ZIJIMO == type){
					List<Integer> pengGang = checkPengGang(p, pai);
					if(pengGang.size() != 0){
						actionList.addAll(pengGang);
					}
					
				}
				//自摸的时候,检测能不能暗杠.
				if(isShouBaYi(p, type, Cnst.ACTION_TYPE_ANGANG) && Cnst.CHECK_TYPE_ZIJIMO == type ){
					List<Integer> checkAnGang = checkAnGang(p,room);
					for (int i = 0; i < checkAnGang.size(); i++) {
						actionList.add(checkAnGang.get(i));
					}
				}
				//检测是否可以地胡 此时不用听
				if(checkIsDiHu(room)){
					if (checkHuRule(p,room,pai,type)) {
						actionList.add(500);	
					}
				}
				
			}
		}else if(type == Cnst.CHECK_TYPE_BIERENCHU){//别人出
			if (ting) {
				if (checkHuRule(p,room,pai,type)) {
					actionList.add(500);	
				} 
				//不是自摸,检测别人出牌的时候,能不能点杠.
				if (isShouBaYi(p, type, Cnst.ACTION_TYPE_DIANGANG) && Cnst.CHECK_TYPE_ZIJIMO != type && checkTingGang(p, room,pai,3)) {
					actionList.add(pai+90);
					tingGang = true;
				}
			}else{
				if (isShouBaYi(p, type, Cnst.ACTION_TYPE_CHI) &&  type != Cnst.CHECK_TYPE_ZIJIMO  && checkChi(p, pai)) {//吃
					List<Integer> c = chi(p, pai);
					if (!checkChi) {//不让检测吃
						if (type.equals(Cnst.CHECK_TYPE_BIERENCHU)) {//别人出的时候，不让检测出时，要检测能否吃听
							c = checkChiTing(c, p,pai,room);
							if (c!=null) {
								actionList.addAll(c);
								canTing = true;
							}
						}
					}					
				}
				if (isShouBaYi(p, type, Cnst.ACTION_TYPE_PENG) && type != Cnst.CHECK_TYPE_ZIJIMO  &&  checkPeng(p, pai)) {//碰
					Integer peng = peng(p, pai);
					actionList.add(peng);	
					//检测是否能碰听
					if (checkPengTing(peng, p, pai, room)) {
						canTing = true;
						actionList.add(peng+140);
					}
				}
				//不是自摸,检测别人出牌的时候,能不能点杠.
				if (isShouBaYi(p, type, Cnst.ACTION_TYPE_DIANGANG) && Cnst.CHECK_TYPE_ZIJIMO != type && checkDianGang(p, pai)) {
					Integer gang = gang(p, pai,false);
					actionList.add(gang);
				}
				//检测是否可以地胡 此时不用听
				if(checkIsDiHu(room)){
					if (checkHuRule(p,room,pai,type)) {
						actionList.add(500);	
					}
				}
			}
		}else if(type == Cnst.CHECK_TYPE_QIANGGANG){//抢杠胡
			if (ting&&checkHuRule(p,room,pai,type)) {
				actionList.add(500);	
			}
		}else if(type == Cnst.CHECK_TYPE_HAIDIANPAI){//海底牌
			if (ting&&checkHuRule(p,room,pai,type)) {
				actionList.add(500);	
			}
		}else if(type == Cnst.CHECK_TYPE_TIANHU){
			if (checkHuRule(p,room,pai,type)) {
				actionList.add(500);	
			}
			//检测暗杠
			if(true){
				List<Integer> checkAnGang = checkAnGang(p,room);
				for (int i = 0; i < checkAnGang.size(); i++) {
					actionList.add(checkAnGang.get(i));
				}
			}
		}
		//胡牌不让点过 有杠可以点过 天胡地胡可以点过
		if (actionList.size() != 0) {
			if(canTing){
				actionList.add(Cnst.ACTION_BIANMA_BAOLOU);
			}
			//听牌 
			if(ting && actionList.contains(500) && !tingGang ){
				
			}else{
				actionList.add(0);
			}
		}else{
			//没有动作 只能出牌
			if(type == Cnst.CHECK_TYPE_ZIJIMO || type == Cnst.CHECK_TYPE_TIANHU){
				actionList.add(501);
			}		
		}		
		return actionList;
	}
	/**
	 * 检测是否符合地胡要求
	 * @param room
	 * @return
	 */
	public static boolean checkIsDiHu(RoomResp room){
		return room.isDiHu() && room.getLastAction()!=null;
	}
	
	private static boolean checkPengTing(Integer oldAction,Player p,Integer pai,RoomResp room){
		List<Integer> newPais = getNewList(p.getCurrentMjList());
		int[] pais = getCheckHuPai(newPais, null);
		pais[pai-1]-=2;
		List<Integer> temp = getListFromArray(pais);
		p.setCurrentMjList(temp);
		if (checkTing(p, room,Cnst.CHECK_PENGTING)) {
			p.setCurrentMjList(newPais);
			return true;
		}

		p.setCurrentMjList(newPais);
		return false;
	}
	
	private static List<Integer> checkChiTing(List<Integer> oldChiActions,Player p,Integer pai,RoomResp room){
		List<Integer> list = new ArrayList<Integer>();
		List<Integer> newPais = getNewList(p.getCurrentMjList());
		//c中放的是行为编码
		for (int n = 0; n < oldChiActions.size(); n++) {
			int[] pais = getCheckHuPai(newPais, null);
			int[] chiDePais = Cnst.chiMap.get(oldChiActions.get(n));
			for (int m = 0; m < chiDePais.length; m++) {//chiDePais里面装的就是1-34
				if (chiDePais[m]!=pai) {
					pais[chiDePais[m]-1]--;
				}
			}
			List<Integer> temp = getListFromArray(pais);
			p.setCurrentMjList(temp);
			if (checkTing(p, room,Cnst.CHECK_CHITING)) {
				list.add(oldChiActions.get(n)+140);
			}
		}
		p.setCurrentMjList(newPais);
		
		return list.size()>0?list:null;
	}
	
	private static List<Integer> getListFromArray(int[] pais){
		List<Integer> list = new ArrayList<Integer>();
		for (int n = 0; n < pais.length; n++) {
			if (pais[n]!=0) {
				for (int a = 0; a < pais[n]; a++) {
					list.add(n+1);
				}
			}
		}
		return list;
		
	}
	

	/**
	 * 检测能不能碰完以后再开杠.
	 * @param p
	 * @return
	 */
	private static List<Integer> checkPengGang(Player p, Integer pai) {
		List<Action> actionList = p.getActionList();//统计用户所有动作 (吃碰杠等)
		List<Integer> newList = getNewList(p.getCurrentMjList());
		List<Integer> gangList = new ArrayList<Integer>();
		for (int i = 0; i < actionList.size(); i++) {
			if(actionList.get(i).getType() == 2){
				for(int m=0;m<newList.size();m++){
					if(newList.get(m) == actionList.get(i).getExtra()){
						gangList.add(newList.get(m)+90);
					}
				}
			}
		}
		return gangList;
	}
	
	/**
	 * 手把一
	 * @param p
	 * @param type
	 * @return
	 */
	public static boolean isShouBaYi(Player p,Integer type,Integer actionType){
		//有暗杠不能手把一
		boolean hasAnGang = false;
		if(type == Cnst.ACTION_TYPE_ANGANG){
			hasAnGang = true;
		}
		if(!hasAnGang){
			List<Action> actions = p.getActionList();
			in : if(actions!=null && actions.size() > 0){
				for(Action action:actions){
					if(action.getType() == Cnst.ACTION_TYPE_ANGANG){
						hasAnGang = true;
						break in;
					}
				}
			}
		}
		if(hasAnGang && p.getCurrentMjList().size() <= 5){
			return false;
		}
		return true;
	}


	/***
	 * 根据出的牌 设置下个动作人和玩家
	 * @param players
	 * @param room
	 * @param pai
	 */
	public static void getNextAction(List<Player> players, RoomResp room, Integer pai){
		Integer maxAction = 0;
		Long nextActionUserId = -1L;
		List<Integer> nextAction = new ArrayList<Integer>();
		int index = -1;
		Long[] playIds = room.getPlayerIds();
		for(int i=0;i<playIds.length;i++){
			if(playIds[i].equals(room.getLastChuPaiUserId())){
				index = i+1;
				if(index == playIds.length){
					index = 0;
				}
				break;
			}
		}
		Long xiaYiJia = playIds[index];
		//从下一家开始检测 多胡的话 会按顺序来
		Player[] checkList = new Player[players.size()];
		for(int i=0;i<players.size();i++){
			if(i == index){
				checkList[0] = players.get(i);
			}
			if(i<index){
				checkList[checkList.length-(index-i)] = players.get(i);
			}
			if(i>index){
				checkList[i-index] = players.get(i);
			}
		}
		for(Player p:checkList){
			if(p!=null&&!room.getGuoUserIds().contains(p.getUserId())){
				//玩家没点击过 或者不是 出牌的人  吃只检测下个人
				List<Integer> checkActionList;
				if(p.getUserId().equals(xiaYiJia)){
					checkActionList = checkActionList(p, pai, room,Cnst.CHECK_TYPE_BIERENCHU,false);
				}else{
					checkActionList = checkActionList(p, pai, room,Cnst.CHECK_TYPE_BIERENCHU,false);
				}
				
				if(checkActionList.size() == 0){
					//玩家没动作 
					room.getGuoUserIds().add(p.getUserId());
				}else{
					Collections.sort(checkActionList);
					//判断多胡
					if(checkActionList.contains(500)){
						room.getCanWinPlayerIds().add(p.getUserId());
					}
					if(checkActionList.get(checkActionList.size()-1) > maxAction){
						nextActionUserId = p.getUserId();
						nextAction = checkActionList;
						maxAction = checkActionList.get(checkActionList.size()-1);
					}
				}
			}
		}
		//如果都没可执行动作 下一位玩家请求发牌
		if(maxAction == 0){
			nextAction.add(-1);
			room.setNextAction(nextAction);
			//取到上个出牌人的角标 下一位来发牌
			room.setNextActionUserId(xiaYiJia);
		}else{
			room.setNextAction(nextAction);
			room.setNextActionUserId(nextActionUserId);
		}
	
	}

	/**
	 * 检查玩家能不能碰
	 * 
	 * @param p
	 * @param Integer
	 *            peng 要碰的牌
	 * @return
	 */
	public static boolean checkPeng(Player p, Integer peng) {
		int num = 0;
		for (Integer i : p.getCurrentMjList()) {
			if(i == peng){
				num++;
			}
		}
		if (num >= 2) {
			return true;
		}
		return false;
	}

	/**
	 * //与吃的那个牌能组合的List
	 * @param p
	 * @param chi
	 * @return
	 */
	public static List<Integer> reChiList(Integer action ,Integer chi){
		ArrayList<Integer> arrayList = new ArrayList<Integer>();
		for (int i = 35; i <= 56; i++) {
			if(i == action ){
				int[] js = Cnst.chiMap.get(action);
				for (int j = 0; j < js.length; j++) {
					if(js[j] != chi){
						arrayList.add(js[j]);
					}
				}
			}
		}
		return arrayList; 
	}
	
	/**
	 * 执行动作吃!
	 * 返回原本手里的牌
	 * @param p
	 * @param chi
	 * @return
	 */
	public static List<Integer> chi(Player p, Integer chi) {
		List<Integer> shouPai = getNewList(p.getCurrentMjList());
		Set<Integer> set = new HashSet<Integer>();
		List<Integer> reList = new ArrayList<Integer>();
		boolean a = false; // x<x+1<x+2
		boolean b = false; // x-1<x<x+1
		boolean c = false; // x-2<x-1<x

		// 万
		if (chi < 10) { // 基数34
			List<Integer> arr = new ArrayList<Integer>();
			arr.add(chi + 1);
			arr.add(chi + 2);
			if (shouPai.containsAll(arr)) {
				a = true;
			}
			List<Integer> arr1 = new ArrayList<Integer>();
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (shouPai.containsAll(arr1)) {
				b = true;
			}
			List<Integer> arr2 = new ArrayList<Integer>();
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (shouPai.containsAll(arr2)) {
				c = true;
			}

			if (a && chi != 9 && chi != 8) {
				set.add(34 + chi);
			}
			if (b && chi != 9) {
				set.add(33 + chi);
			}
			if (c) {
				set.add(32 + chi);
			}

			// 饼
		} else if (chi >= 10 && chi <= 18) { // 基数32
			List<Integer> arr = new ArrayList<Integer>();
			arr.add(chi + 1);
			arr.add(chi + 2);
			if (shouPai.containsAll(arr)) {
				a = true;
			}
			List<Integer> arr1 = new ArrayList<Integer>();
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (shouPai.containsAll(arr1)) {
				b = true;
			}
			List<Integer> arr2 = new ArrayList<Integer>();
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (shouPai.containsAll(arr2)) {
				c = true;
			}
			if (a & chi != 18 && chi != 17) {
				set.add(32 + chi);
			}
			if (b && chi != 10 && chi != 18) {
				set.add(31 + chi);
			}
			if (c && chi != 10 && chi != 11) {
				set.add(30 + chi);
			}
			// 条
		} else if (chi >= 19 && chi <= 27) { // 基数30
			List<Integer> arr = new ArrayList<Integer>();
			arr.add(chi + 1);
			arr.add(chi + 2);
			if (shouPai.containsAll(arr)) {
				a = true;
			}
			List<Integer> arr1 = new ArrayList<Integer>();
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (shouPai.containsAll(arr1)) {
				b = true;
			}
			List<Integer> arr2 = new ArrayList<Integer>();
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (shouPai.containsAll(arr2)) {
				c = true;
			}
			if (a & chi != 26 && chi != 27) {
				set.add(30 + chi);
			}
			if (b && chi != 19 && chi != 27) {
				set.add(29 + chi);
			}
			if (c && chi != 19 && chi != 20) {
				set.add(28 + chi);
			}
		}
		reList.addAll(set);
		return reList;
	}
	/**
	 * 执行动作杠
	 * 
	 * @param p
	 * @param gang
	 * @return
	 */
	public static Integer gang(Player p, Integer gang, Boolean pengGang) {
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		
		if(pengGang){
			List<Action> actionList = p.getActionList();//统计用户所有动作 (吃碰杠等)
			for (int i = 0; i < actionList.size(); i++) {
				if(actionList.get(i).getType() == 2 && actionList.get(i).getExtra() == gang){
					return 90 + gang;
				}
			}
		}

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 3) { // 控制有几个重复的
				// System.out.println(key + "有重复的:" + map.get(key).intValue() +
				// "个 ");
				if (key == gang) {
					return 90 + gang;
				}
			}
		}

		return -100;
	}

	/**
	 * 执行动作碰
	 * 
	 * @param p
	 * @param peng
	 * @return 行为编码
	 */
	public static Integer peng(Player p, Integer peng) {
		return 56 + peng;
	}

	/**
	 *  * 检测玩家能不能吃.10 与19特殊处理
	 * @param p
	 * @param chi
	 * @param hunPai 不能吃
	 * @return
	 */
	public static boolean checkChi(Player p, Integer chi) {
		List<Integer> list = getNewList(p.getCurrentMjList());
		boolean isChi = false;
		List<Integer> arr = new ArrayList<Integer>();
		arr.add(chi + 1);
		arr.add(chi + 2);
		if (list.containsAll(arr)) {
			isChi = true;
		}
		List<Integer> arr1 = new ArrayList<Integer>();
		List<Integer> arr2 = new ArrayList<Integer>();
		if (chi != 10 && chi != 19) {
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (list.containsAll(arr1)) {
				isChi = true;
			}
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (list.containsAll(arr2)) {
				isChi = true;
			}
		}
		return isChi;
	}

	/**
	 * 执行暗杠
	 * 
	 * @param p
	 * @return 返回杠的牌
	 */
	public static Integer anGang(Player p) {
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		Integer gang = 0;
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 4) { // 控制有几个重复的
				// System.out.println(key + "有重复的:" + map.get(key).intValue() +
				// "个 ");
				gang = key;
			}
		}

		Iterator<Integer> iter1 = p.getCurrentMjList().iterator();
		while (iter1.hasNext()) {
			Integer item = iter1.next();
			if (item == gang) {
				iter1.remove();
			}
		}
		return gang + 90;
	}

	/**
	 * 检查能不能暗杠
	 * 
	 * @param p
	 * @param gang
	 * @return
	 */
	public static List<Integer> checkAnGang(Player p,RoomResp room) {
		List<Integer> anGangList = new ArrayList<Integer>();
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 4 ) { // 控制有几个重复的
				 anGangList.add(key+90);
			}
		}
		return anGangList;
	}

	/**
	 * 检测玩家能不能点杠
	 * 
	 * @param p
	 * @return
	 */
	public static boolean checkDianGang(Player p, Integer gang) {
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 3) { // 控制有几个重复的;
				if (key == gang) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * @param mahjongs
	 *            房间内剩余麻将的组合
	 * @param num
	 *            发的张数
	 * @return
	 */
	public static List<Integer> faPai(List<Integer> mahjongs, Integer num) {
		if (mahjongs.size() == 0) {
			return null;
		}
		List<Integer> result = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			result.add(mahjongs.get(i));
			mahjongs.remove(i);
		}
		return result;
	}
	
	
	
	/**
	 * 返回一个新的集合
	 * @param old
	 * @return
	 */
	public static List<Integer> getNewList(List<Integer> old) {
		List<Integer> newList = new ArrayList<Integer>();
		if (old != null && old.size() > 0) {
			for (Integer pai : old) {
				newList.add( pai );
			}
		}
		return newList;
	}

//	/**
//	 * 丹阳推到胡规则 返回的是分
//	 * 
//	 * @param players
//	 * @param room
//	 * @return
//	 */
//	public static int checkHuFenInfo(List<Player> players,RoomResp room) {
//		Player p = null;
//		//胡牌就是1分
//		int fen = 1;
//		List<Integer> winInfo = new ArrayList<Integer>();
//		for (Player player : players) {
//			if (player.getIsHu()) {
//				player.setHuNum(player.getHuNum() + 1);
//				p = player;
//			}
//		}		
//
//		if(room.getZhuangId().equals(p.getUserId())){
//			winInfo.add(Cnst.ZHUANG);
//		}
//		//清一色 
//		if(isQingYiSe(p, room, p.getCurrentMjList())){
//			winInfo.add(Cnst.QINGYISE);
//			fen = fen * 4;
//		}
//		List<Integer> newList = getNewList(p.getCurrentMjList());
//		//飘
//		if(isPengPengHu(p, newList, 0)){
//			winInfo.add(Cnst.PIAO);
//			fen = fen * 4;
//		}else{
//			//夹胡
//			if(checkKaBianDiao(p, room)){
//				winInfo.add(Cnst.JIAHU);
//				fen = fen * 2;
//				Integer huDePai = p.getCurrentMjList().get(p.getCurrentMjList().size()-1);
//			}
//		}
//		
//		if(!winInfo.contains(Cnst.JIAHU)&&!winInfo.contains(Cnst.QINGYISE)&&!winInfo.contains(Cnst.PIAO)&&!winInfo.contains(Cnst.QIXIAODUI)&&!winInfo.contains(Cnst.TE)){
//			winInfo.add(Cnst.PINGHU);
//		}
//		//门清两番
//		if(p.getActionList()!=null && p.getActionList().size() == 0){
//			fen = fen * 2;
//			winInfo.add(Cnst.MENQING);
//		}
//		//自摸两番
//		if(p.getIsZiMo()){
//			fen = fen * 2;
//			winInfo.add(Cnst.ZIMO);
//		}else{
//			winInfo.add(Cnst.DIANPAO);
//		}
//		p.setFanShu(winInfo);
//		
//		return fen;
//	}
	
	

	/**
	 * 从牌桌上,把玩家吃碰杠的牌移除.
	 * @param room
	 * @param players
	 */
	
	public static void removeCPG(RoomResp room, List<Player> players) {
		Player currentP = null;
		for (Player p : players) {
			if(p.getUserId().equals(room.getLastChuPaiUserId())){
				currentP = p;
				List<Integer> chuList = p.getChuList();
				Iterator<Integer> iterator = chuList.iterator();
				while(iterator.hasNext()){
					Integer pai = iterator.next();
					if(room.getLastChuPai() == pai ){
						iterator.remove();
						break;
					}
				}
			}
		}
		RedisUtil.updateRedisData(null, currentP);
	}
	
	/***
	 * 移除动作手牌 
	 * @param currentMjList
	 * @param chi
	 * @param action
	 * @param type
	 */
	public static void removeActionMj(List<Integer> currentMjList,List<Integer> chi,Integer action,Integer type){
		Iterator<Integer> it = currentMjList.iterator(); //遍历手牌,删除碰的牌
		switch (type) {
		case Cnst.ACTION_TYPE_CHI:
			int chi1 = 0;
			int chi2 = 0;
			a : while(it.hasNext()){
					Integer x = it.next();
					if(x == chi.get(0) && chi1 == 0){
						it.remove();
						chi1 = 1 ;
					}
					if(x == chi.get(1) && chi2 == 0){
						it.remove();
						chi2 = 1;
					}
					if(chi1 == 1 && chi2 == 1){
						break a;
					}
				}		
			break;
		case Cnst.ACTION_TYPE_PENG:
			int num = 0;
			while(it.hasNext()){
				Integer x = it.next();
			    if(x==action-56){
			        it.remove();
			        num = num + 1;
			        if(num == 2){
			        	break;
			        }
			    }
			}
			break;
		case Cnst.ACTION_TYPE_ANGANG:
			List<Integer> gangPai = new ArrayList<Integer>();
			gangPai.add(action-90);
			currentMjList.removeAll(gangPai);
			break;
		case Cnst.ACTION_TYPE_PENGGANG:
			gangPai = new ArrayList<Integer>();
			gangPai.add(action-90);
			currentMjList.removeAll(gangPai);
			break;
		case Cnst.ACTION_TYPE_DIANGANG:
			gangPai = new ArrayList<Integer>();
			gangPai.add(action-90);
			currentMjList.removeAll(gangPai);
			break;
		default:
			break;
		}
	}
	/***
	 * 获得 检测胡牌的 34位数组 包括摸得或者别人打的那张
	 * @param currentList
	 * @param pai
	 * @return
	 */
	public static int[] getCheckHuPai(List<Integer> currentList,Integer pai){
		int[] checkHuPai = new int[34];
		List<Integer> newList = getNewList(currentList);
		if(pai!=null){
			newList.add(pai);
		}
		for(int i=0;i<newList.size();i++){
			int a = checkHuPai[newList.get(i) - 1];
			checkHuPai[newList.get(i) - 1] = a + 1;
		}
		return checkHuPai;
	}

	/***
	 * 获得 检测胡牌的 34位数组 不包括摸得或者别人打的那张
	 * @param currentList
	 * @param pai
	 * @return
	 */
	public static int[] getRemoveLastPai(List<Integer> currentList,Integer pai){
		int[] checkHuPai = new int[34];
		Boolean hasRemove = false; 
		for(int i=0;i<currentList.size();i++){
			if(currentList.get(i) == pai && !hasRemove){
				hasRemove = true;
				continue;
			}
			int a = checkHuPai[currentList.get(i) - 1];
			checkHuPai[currentList.get(i) - 1] = a + 1;
		}
		return checkHuPai;
	}
	
	/**
	 *  牌型是否是清一色 红中不算门 ,单门+红中胡牌算是清一色
	 * 
	 * @param p玩家
	 * @return
	 */
	public static boolean isQingYiSe(Player p , RoomResp room ,List<Integer> list) {
		Integer leixing=0;
		Boolean needcheck=false;
		List<Integer> newList = getNewList(list);
		//如果最后一个是宝牌 移除掉
		if(newList.get(newList.size()-1) == room.getBaoPai()){
			newList.remove(newList.size()-1);
		}
		Collections.sort(newList);
		Integer pai = newList.get(0);
		leixing=(pai-1)/9;
		if(leixing==3){//单调红中，只看吃碰杠类型就Ok
			needcheck=true;
		}else{//不是单调红中
			for (Integer shouPai : newList) {
				//红中跳出
				if((shouPai-1)/9==3){
					continue;
				}
				//要检测的类型不再相同
				if(leixing!=(shouPai-1)/9){
					return false;
				}
			}
		}
		//判断有动作的牌类型是否相同
		Integer extra=0;
		List<Action> actionList = p.getActionList();
		if(actionList.size()>0){
			if(needcheck){//绝对不会大于3了，因为红中只能碰一次
				leixing=(actionList.get(0).getExtra()-1)/9;
				needcheck=false;
			}
			for (Action action : actionList) {
				extra = action.getExtra();
				if(extra>=28){
					continue;
				}else{
					if(leixing!=(extra-1)/9){
						return false;
					}
				}
			}
		}
		return true;
	}
	
	
	
	public static void checkHuFenInfo(List<Player> players, RoomResp room){
		//先重置加飘和暴漏
		
		for(Player p:players){
			if(p.isBaoLou()){
				p.getFanShu().add(Cnst.HU_TYPE_BAOLOU);
			}
			if(room.getPlayType() == 1 && (p.getPiao() == 3 || p.getPiao() == 4)){
				p.getFanShu().add(Cnst.HU_TYPE_JIAPIAO);
			}
		}
		for(Long userId:room.getWinPlayerIds()){
			Player p = null;
			in:for(Player player:players){
				if(player.getUserId().equals(userId)){
					p = player;
					break in;
				}
			}
			int fen = 1;
			
			// 清一色
			if (isQingYiSe(p, room, p.getCurrentMjList())) {
				p.getFanShu().add(Cnst.HU_TYPE_QINGYISE);
				fen = fen * 2;
			}
			
			boolean moBao = false;
			if(p.getIsZiMo() && p.getTing() == Cnst.TING_STATE_2 && p.getCurrentMjList().get(p.getCurrentMjList().size()-1) == room.getBaoPai()){
				moBao = true;
			}
			// 检测地胡
			if(checkIsDiHu(room)){
				p.getFanShu().add(Cnst.HU_TYPE_DIHU);
				fen = fen * 2;
			}
			// 检测天胡
			if(room.getLastAction() == null){
				p.getFanShu().add(Cnst.HU_TYPE_TIANHU);
				fen = fen * 2;
			}
			//检测 飘胡 七对 豪七
			//获取全部手牌的数量
			int size = p.getCurrentMjList().size();
			
			// 防止原来的牌发生变化
			List<Integer> newList = getNewList(p.getCurrentMjList());
			// 获取动作牌
			Integer dongZuoPai =  p.getCurrentMjList().get(size - 1);
			// 移除动作牌
			newList.remove(size - 1);
			
			int[] shouPaiArr = listToArray(newList);
			if(checkPiao(shouPaiArr, moBao, dongZuoPai,p)){
				p.getFanShu().add(Cnst.HU_TYPE_PIAOHU);
				fen = fen * 2;
			}
			if(p.getCurrentMjList().size() == 14){
				int qiDui = checkQiDui(shouPaiArr, moBao, dongZuoPai
						);
				if(qiDui == 0){
					p.getFanShu().add(Cnst.HU_TYPE_QIDUI);
					fen = fen * 2;
				}
				if(qiDui > 0){
					p.getFanShu().add(Cnst.HU_TYPE_HAOQI);
					fen = fen * 4;
					//自摸算暗杠 点炮算明杠
//					if(p.getIsZiMo()){
//						p.setGangScore(p.getGangScore() + room.getPlayerNum()-1);
//						for(Player player:players){
//							if(player.getUserId().equals(p.getUserId())){
//								continue;
//							}
//							player.setGangScore(player.getGangScore() - 1);
//						}
//					}else{
//						p.setGangScore(p.getGangScore() + 1);
//						a:for(Player player : players){
//							if(player.getIsDian()){
//								player.setGangScore(player.getGangScore() - 1);
//								break a;
//							}
//						}
//					}
				}
			}
					
			
			if(!p.getFanShu().contains(Cnst.HU_TYPE_HAOQI) && 
				!p.getFanShu().contains(Cnst.HU_TYPE_PIAOHU) && 
				!p.getFanShu().contains(Cnst.HU_TYPE_QIDUI) && 
				!p.getFanShu().contains(Cnst.HU_TYPE_QINGYISE)){
				p.getFanShu().add(Cnst.HU_TYPE_PINGHU);
			}
			//算分
			if(p.isMoBao()){
				p.getFanShu().add(Cnst.HU_TYPE_TONGBAO);
			}
			if(p.getIsZiMo()){
				//三家付
				int zongFen = 0;
				for(Player shuDeRen:players){
					if(shuDeRen.getUserId().equals(p.getUserId())){
						continue;
					}
					int shuFen = fen;
					if(shuDeRen.isBaoLou() || p.isBaoLou()){
						shuFen = shuFen * 2;
					}
					if(shuDeRen.getPiao() == 3 || shuDeRen.getPiao() == 4){
						shuFen = shuFen + 1;
					}
					if(p.getPiao() == 3 || p.getPiao() == 4){
						shuFen = shuFen + 1;
					}
					shuDeRen.setThisScore(shuDeRen.getThisScore()-shuFen);
					zongFen += shuFen;
				}
				p.setThisScore(p.getThisScore()+zongFen);
			}else{
				//谁点谁付
				int shuFen = fen;
				Player dianDeRen = null;
				b:for(Player dian:players){
					if(dian.getIsDian()){
						dianDeRen = dian;
						break b;
					}
				}
				if(dianDeRen.isBaoLou() || p.isBaoLou()){
					shuFen = shuFen * 2;
				}
				if(dianDeRen.getPiao() == 3 || dianDeRen.getPiao() == 4){
					shuFen = shuFen + 1;
				}
				if(p.getPiao() == 3 || p.getPiao() == 4){
					shuFen = shuFen + 1;
				}
				dianDeRen.setThisScore(dianDeRen.getThisScore()-shuFen);
				p.setThisScore(p.getThisScore()+shuFen);
			}	
		}
		//统计总分
		for(Player p:players){
			p.setScore(p.getScore()+p.getThisScore()+p.getGangScore());
		}
	}
	

	public static List<Integer> paiXu(List<Integer> pais) {
		Collections.sort(pais);
		return pais;
	}

	/**
	 * 检查能不能听牌后杠
	 * 
	 * @param p
	 * @param gang
	 * @return
	 */
	public static boolean checkTingGang(Player p,RoomResp room,Integer pai,Integer num) {
		List<Integer> shouPai = p.getCurrentMjList();
		boolean checkHu = false;
		Integer hasNum = 0;
		for(Integer pa:shouPai){
			if(pa.equals(pai)){
				hasNum ++ ;
			}
			if(hasNum == num){
				checkHu = true;
			}
		}
		if(checkHu){
			//除去这几张牌 还是听牌	
			if (checkGangTing(p, room,pai)) {
				return true;
			}else{
				return false;
			}
		}
		return false;
	}

	
	
	/**
	 * 
	 * @Title: 杠牌后还能听   
	 * @Description: 手牌个数一定是3n+2
	 * @param: @param p
	 * @param: @param room
	 * @param: @return      
	 * @return: boolean      
	 * @throws
	 */
	public static boolean checkGangTing(Player p,RoomResp room,Integer pai){
		List<Integer> newList = getNewList(p.getCurrentMjList());
		newList.add(34);//加一个混
		int[] pais = getCheckHuPai(newList, null);
		pais[pai-1] = 0;//去了这张牌还能听
//		for (int n = 0; n < pais.length; n++) {
//			if (pais[n]>0&&n!=33) {  
//				pais[n]--;
				if (Hulib.getInstance().get_hu_info(pais, 34, 33)) {
					return true;
				}
//				pais[n]++;
//			}
//		}
		return false;
	}
	
	
	/**
	 * 这个方法只能检测手牌
	 * @param shouPaiArr
	 * @param hunNum
	 * @param hunPai
	 * @param dongZuoPai
	 */
	private static Boolean checkPiao(int[] shouPaiArr, boolean moBao, Integer dongZuoPai,Player p) {
		// 检测动作里面是否有刻
		List<Action> actionList = p.getActionList();
		// 1吃 2碰 3点杠 4碰杠 5暗杠
		for (Action action : actionList) {
			if (action.getType() == 1) {
				return false;
			}
		}
		int nowHunNum=0;
		int x=shouPaiArr[dongZuoPai-1];
		if(moBao){
			nowHunNum++;
		}else{
			shouPaiArr[dongZuoPai-1]=x+1;
		}
		int oneNum=0;
		int twoNum=0;
		boolean isPaio=true;
		// 2： 剩下的牌必须组成砰砰
		for (int i : shouPaiArr) {
			if (i == 4) {
				//拆成3和1
				oneNum++;
			} else if (i == 1) {// 1
				oneNum++;
			} else if (i == 2) {
				twoNum++;
			}
		}
		shouPaiArr[dongZuoPai-1]=x;
		if(isPaio){
			if ((twoNum - 1 + 2 * oneNum) <= nowHunNum) {
				return true;
			}
		}
		return false;
		
	}
	
	/**
	 * 检测七对 --此方法会对绝选项进行区分检测
	 * 
	 * @param shouPaiArr
	 *            手牌去除混的牌 而 转变成的数组
	 * @param hunNum
	 *            混牌的数量 --不带混的话就输入1
	 * @param dongZuoPai 
	 * @param dongZuoPai 
	 * @return 0 七对 1 七对(有1个4张的) 2 七对(有2个4张的) 3 七对(有3个4张的) -1不是七对
	 */
	private static Integer checkQiDui(int[] shouPaiArr, boolean moBao, Integer dongZuoPai) {
		int nowHunNum=0;
		int x=shouPaiArr[dongZuoPai-1];
		if(moBao){
			nowHunNum++;
		}else{
			shouPaiArr[dongZuoPai-1]=x+1;
		}
		int oneNum = 0;
		int threeNum = 0;
		int fourNum = 0;
		for (int i = 0; i < shouPaiArr.length; i++) {
			if (shouPaiArr[i] == 4) {
				fourNum++;
			} else if (shouPaiArr[i] == 3) {
				threeNum++;
			} else if (shouPaiArr[i] == 2) {
			} else if (shouPaiArr[i] == 1) {
				oneNum++;
			}
		}
		// 检测是否其对
		shouPaiArr[dongZuoPai-1]=x;
		if ((oneNum + threeNum) > nowHunNum) {
			return -1;
		} else {
			if(threeNum > 0){
				fourNum += threeNum;
				nowHunNum -= threeNum;
			} 
			if(oneNum > 0){
				fourNum += nowHunNum/3;
			}
			return fourNum;
		}
	}
	
	public static void main(String[] args) {
		int[] a = new int[33];
		a[0] = 2;
		a[1] = 2;
		a[2] = 2;
		a[3] = 2;
		a[4] = 2;
		a[5] = 2;
		a[6] = 2;
		a[7] = 1;
		
		System.out.println(checkQiDui(a, false, 9));
	}
	/**
	 * 将手牌变成数组--此方法转换出来的是带混的手牌
	 * 
	 * @param shouPaiList
	 * @return
	 */
	private static int[] listToArray(List<Integer> shouPaiList) {
		int[] shouPaiArr = new int[34];
		for (Integer integer : shouPaiList) {
			int num = shouPaiArr[integer - 1];
			shouPaiArr[integer - 1] = num + 1;
		}
		return shouPaiArr;
	}
	
	/**
	 * 检测七对 --此方法会对绝选项进行区分检测
	 * 
	 * @param shouPaiArr
	 *            手牌去除混的牌 而 转变成的数组
	 * @param hunNum
	 *            混牌的数量 --不带混的话就输入1
	 * @param dongZuoPai 
	 * @param dongZuoPai 
	 * @return 0 七对 1 七对(有1个4张的) 2 七对(有2个4张的) 3 七对(有3个4张的) -1不是七对
	 */
	public static boolean checkHuTingQiDui(int[] shouPaiArr, boolean ting,Player p,Integer type) {
		if(type == Cnst.CHECK_CHITING || type == Cnst.CHECK_PENGTING){
			return false;
		}
		if(p.getActionList()!=null && p.getActionList().size()>0){
			return false;
		}
		int nowHunNum = 0;
		if(ting){
			//检测听
			nowHunNum = 1;
		}
		int oneNum = 0;
		int threeNum = 0;
		for (int i = 0; i < shouPaiArr.length; i++) {
			if(i == 33){
				continue;
			}
			if (shouPaiArr[i] == 4) {
				
			} else if (shouPaiArr[i] == 3) {
				threeNum++;
			} else if (shouPaiArr[i] == 2) {
			} else if (shouPaiArr[i] == 1) {
				oneNum++;
			}
		}
		// 检测是否其对
		if ((oneNum + threeNum) > nowHunNum) {
			return false;
		} else {
			return true;
		}
	}
}
