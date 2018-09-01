package com.yzt.logic.util.GameUtil;


import java.util.ArrayList;
import java.util.List;

import com.yzt.logic.mj.function.TCPGameFunctions;
import com.yzt.logic.util.JudegHu.checkHu.Hulib;
import com.yzt.logic.util.JudegHu.checkHu.TableMgr;

public class Demo extends TCPGameFunctions {
	
	static {
		// 加载胡的可能
		TableMgr.getInstance().load();
	}
	/**
	 * --这个方法的前提是它可以胡
	 * 要求精准的检测出：边（只包含12胡3，89胡7）、夹（普通的夹胡）、吊、对倒飘、单吊飘、七对（包含豪华等，只要符合7对就行
	 * ），每个胡法一个方法，无混，所有数据含义基于通用的行为编码，具体要求如下
	 * 1、所有传入参数都用基本数据类型，不要用Player或者RoomResp对象
	 * 2、新建java类，自定义传入参数个数以及含义，返回true/false
	 * 3、要求详细备注，备注清楚传入参数以及含义，各个返回值的含义，以及代码逻辑处的特殊逻辑标注，后期java人员会一起参与讨论到算法以及效率最优
	 * 4、追求自己的极致，能写到最优算法、最高效最好 5、要求通过大量测试，每个方法至少要百万级次的牌型测试，确保无误
	 * 上述的类型只是通用基本类型，如果还有通用基本类型未统计到，会持续增加，待续...
	 */
	public static void main(String[] args) {
		// 设置手牌集合---无论是自摸还是点炮 自己摸的或者别人出的那张牌都放到最后
		List<Integer> shouPaiList = new ArrayList<Integer>();

		
		shouPaiList.add(7);
		shouPaiList.add(8);
		shouPaiList.add(9);
		shouPaiList.add(8);
		shouPaiList.add(11);
		shouPaiList.add(11);
		shouPaiList.add(11);
		shouPaiList.add(15);
		shouPaiList.add(15);
		shouPaiList.add(15);
		shouPaiList.add(16);
		shouPaiList.add(17);
		shouPaiList.add(18);
		shouPaiList.add(7);
		
		//获取全部手牌的数量
		int size = shouPaiList.size();
		
		// 防止原来的牌发生变化
		List<Integer> newList = getNewList(shouPaiList);
		// 获取动作牌
		Integer dongZuoPai = shouPaiList.get(size - 1);
		// 移除动作牌
		newList.remove(size - 1);
		// 设置混牌 --如果没有请设置成35
		Integer hunPai = 8;
		int[] shouPai = listToArray(newList);
		long currentTimeMillis = System.currentTimeMillis();
		int huNum=0;
//		for (int i = 0; i < 5000000; i++) {
//			if(Hulib.getInstance().get_hu_info(shouPai, 34, hunPai-1)){
//				huNum++;
////				System.out.println("-");
//			}else{
////				System.out.println("++");
//			}
//		}
//		System.out.println(huNum);
//		System.out.println(System.currentTimeMillis()-currentTimeMillis);
//		// 获取混牌数量
		int hunNum = shouPai[hunPai - 1];
		// 将这张混牌置空
		shouPai[hunPai - 1] = 0;
		// 检测吊
		if(checkDiao(shouPai, hunNum, hunPai, dongZuoPai)){
			System.out.println("是单吊");
		}else{
			System.out.println("不是单吊");
		}
		if(size>=5){
			// 检测对倒
			if(checkDuiDao(shouPai, hunNum, hunPai, dongZuoPai)){
				System.out.println("是对倒");
			}else{
				System.out.println("不是对倒");
			}
			// 检测边
			if(checkBian(shouPai, hunNum, hunPai, dongZuoPai)){
				System.out.println("是边胡");
			}else{
				System.out.println("不是边胡");
			}
			// 检测卡=夹
			if(checkKa(shouPai, hunNum, hunPai, dongZuoPai)){
				System.out.println("是卡");
			}else{
				System.out.println("不是卡");
			}
		}
		
		// 检测飘
		if(checkPiao(shouPai, hunNum, hunPai, dongZuoPai)){
			System.out.println("是飘");
		}else{
			System.out.println("不是飘");
		}
		if(size==14){
			// 检测七对
			Integer fourNum = checkQiDui(shouPai, hunNum,hunPai,dongZuoPai);
			if(fourNum==-1){
				System.out.println("没有七对");
			}else{
				System.out.println("是有"+fourNum+"个四个的七对");
			}
			
		}

	}
	/**
	 * 这个方法只能检测手牌
	 * @param shouPaiArr  --此时组不包含混排和最后一张牌
	 * @param hunNum		如果房间规则不带混  hunNum传0 
	 * @param hunPai	如果房间规则不带混 hunPai传手中没有的那张牌，比如手中没有9万传9
	 * @param dongZuoPai  最后摸得那张牌
	 */
	private static Boolean checkPiao(int[] shouPaiArr, int hunNum, Integer hunPai, Integer dongZuoPai) {
		int nowHunNum=hunNum;
		int x=shouPaiArr[dongZuoPai-1];
		if(dongZuoPai==hunPai){
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
	 * 有两个对子
	 * @param shouPaiArr  --此时组不包含混排和最后一张牌
	 * @param hunNum		如果房间规则不带混  hunNum传0 
	 * @param hunPai	如果房间规则不带混 hunPai传手中没有的那张牌，比如手中没有9万传9
	 * @param dongZuoPai  最后摸得那张牌
	 */
	private static Boolean checkDuiDao(int[] shouPaiArr, int hunNum, Integer hunPai, Integer dongZuoPai) {
		if(dongZuoPai==hunPai){
			//设置不重复的手牌集合
			List<Integer> distictJiaoBiaoList=new ArrayList<Integer>();
			for (int i = 0; i < 34; i++) {
				if(shouPaiArr[i]>0){
					distictJiaoBiaoList.add(i);
				}
			}
			int size = distictJiaoBiaoList.size();
			for (int i = 0; i < size; i++) {
				boolean isHu=false;
				int needHunNum=0;
				int erChengHunNum=hunNum;
				Integer jiaoBiao = distictJiaoBiaoList.get(i);
				int jiaoBiaoPaiNum = shouPaiArr[jiaoBiao];
				//这张牌的张数能大于2--移除两张
				if(jiaoBiaoPaiNum+erChengHunNum>=2){
					if(jiaoBiaoPaiNum<2){
						needHunNum=2-jiaoBiaoPaiNum;
						erChengHunNum-=needHunNum;
						shouPaiArr[jiaoBiao]=0;
					}else{
						shouPaiArr[jiaoBiao]=jiaoBiaoPaiNum-2;
					}
				}
				for (int j = i+1; j < size; j++) {
					//通过角标获取这张牌
					Integer erCengjiaoBiao = distictJiaoBiaoList.get(j);
					//获取这张牌的数量
					int sanCengPaiNum = shouPaiArr[erCengjiaoBiao];
					//获取剩下的混的数量
					int sanCengHunNum=erChengHunNum;
					needHunNum=0;
					//这张牌的张数能大于2--移除两张
					if(shouPaiArr[erCengjiaoBiao]+sanCengHunNum>=2 ){
						//减少两张此牌
						if(sanCengPaiNum<2){
							needHunNum=2-sanCengPaiNum;
							sanCengHunNum-=needHunNum;
							shouPaiArr[erCengjiaoBiao]=0;
						}else{
							shouPaiArr[erCengjiaoBiao]=sanCengPaiNum-2;
						}
						sanCengHunNum-=needHunNum;
						shouPaiArr[hunPai-1]=sanCengHunNum;
						if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai-1)){
							isHu=true;
						}
						//将二层角标数据还原
						shouPaiArr[erCengjiaoBiao]=sanCengPaiNum;
						if(isHu){
							break;
						}
					}
				}
				//将数据还原
				shouPaiArr[hunPai-1]=0;
				//将顶层角标数据还原
				shouPaiArr[jiaoBiao]=jiaoBiaoPaiNum;
				if(isHu){
					return true;
				}
			}
		}else{
			//获取原来手中的这张动作牌的数量
			int dongZuoNum = shouPaiArr[dongZuoPai-1];
			//手中这张牌肯定有两张
			int needHunNum=0;
			int erChengHunNum=hunNum;
			//设置手中动作牌现在的数量,并且设置因此减少的混的数量
			if(dongZuoNum+hunNum>=2){
				if(dongZuoNum<2){
					needHunNum=2-dongZuoNum;
					erChengHunNum-=needHunNum;
					shouPaiArr[dongZuoPai-1]=0;
				}else{
					shouPaiArr[dongZuoPai-1]=dongZuoNum-2;
				}
			}
			boolean isHu=false;
			for (int i = 0; i < 34; i++) {
				int sanCengHunNum=erChengHunNum;
				needHunNum=0;
				if(shouPaiArr[i]+sanCengHunNum>=2 &&  i!=dongZuoPai){
					//减少两张此牌
					int j = shouPaiArr[i];
					if(j<2){
						needHunNum=2-j;
						sanCengHunNum-=needHunNum;
						shouPaiArr[i]=0;
					}else{
						shouPaiArr[i]=j-2;
					}
					sanCengHunNum-=needHunNum;
					shouPaiArr[hunPai-1]=sanCengHunNum;
					if(Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai-1)){
						isHu=true;
					}
					//将数据还原
					shouPaiArr[i]=j;
					if(isHu){
						
						break;
					}
				}
			}
			//将数据还原
			shouPaiArr[dongZuoNum]=dongZuoNum;
			shouPaiArr[hunPai-1]=0;
			if(isHu){
				return true;
			}
		}
		return false;
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
	private static Integer checkQiDui(int[] shouPaiArr, int hunNum, Integer hunPai, Integer dongZuoPai) {
		int nowHunNum=hunNum;
		int x=shouPaiArr[dongZuoPai-1];

		if(dongZuoPai.equals(hunPai)){
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
		shouPaiArr[dongZuoPai-1]=x;
		// 检测是否其对
		if ((oneNum + threeNum) > nowHunNum) {
			return -1;
		} else {
			fourNum += nowHunNum - threeNum;
			fourNum += (nowHunNum - threeNum - oneNum) / 2;
			return fourNum;
		}
	}

	/**
	 * 
	 * @param winPlayer
	 * @param hunNum
	 * @param hunPai
	 * @param dongZuoPai
	 * @return 
	 */
	public static Boolean checkKa(int[] shouPaiArr, int hunNum, Integer hunPai, Integer dongZuoPai) {
		boolean isHu=false;
		if(dongZuoPai<28 || dongZuoPai.equals(hunPai) ){
			//防止俩东风，俩混，来个3条或者混检测不出来
			//首先检测手中是否有俩混，有的话，移除能否胡就是夹胡，否则走下面
			if(hunNum>=2){
				shouPaiArr[hunPai-1]=hunNum-2;
				if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
					isHu=true;
				}
				shouPaiArr[hunPai-1]=0;
				if(isHu){
					return true;
				}
			}
		}
		// 动作牌就是混牌
		if (dongZuoPai.equals(hunPai)) {
			List<Integer> needCheckList=new ArrayList<Integer>();
			//找到手中一张牌和大于此牌2的牌
			for (int i = 0; i < 27; i++) {
				int nowHunNum=hunNum;
				int paiNum = shouPaiArr[i];
				if(shouPaiArr[i]>0){
					int paiBig=i%9+1;
					if(paiBig<=2){//1和2
						//找大2的
						int bigTwoJaioBiao=i+2;
						if(needCheckList.contains(i) && needCheckList.contains(bigTwoJaioBiao)){//说明测过这个了
						}else{
							if(!needCheckList.contains(i)){
								needCheckList.add(i);
							}
							if(!needCheckList.contains(bigTwoJaioBiao)){
								needCheckList.add(bigTwoJaioBiao);
							}
							
							int bigTwoNum = shouPaiArr[bigTwoJaioBiao];
							if(nowHunNum+bigTwoNum>0){
								if(bigTwoNum>0){
									shouPaiArr[bigTwoJaioBiao]=bigTwoNum-1;
								}else{
									nowHunNum--;
								}
								//减去夹的这两张牌
								shouPaiArr[i]=paiNum-1;
								shouPaiArr[hunPai-1]=nowHunNum;
								if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
									isHu = true;
								}
								// 没胡,加回来
								shouPaiArr[i] = paiNum;
								shouPaiArr[bigTwoJaioBiao] =bigTwoNum ;
								// 置空混牌数量
								shouPaiArr[hunPai - 1] = 0;
								if (isHu) {
									return true;
								}
							}
						}
					}else if(paiBig>=8){//8和9
						int smallTwoJaioBiao=i-2;
						if(needCheckList.contains(i) && needCheckList.contains(smallTwoJaioBiao)){//说明测过这个了
						}else{
							if(!needCheckList.contains(i)){
								needCheckList.add(i);
							}
							if(!needCheckList.contains(smallTwoJaioBiao)){
								needCheckList.add(smallTwoJaioBiao);
							}
							
							int smallTwoNum=shouPaiArr[smallTwoJaioBiao];
							if(nowHunNum+smallTwoNum>0){
								if(smallTwoNum>0){
									shouPaiArr[smallTwoJaioBiao]=smallTwoNum-1;
								}else{
									nowHunNum--;
								}
								
								shouPaiArr[i]=paiNum-1;
								shouPaiArr[hunPai-1]=nowHunNum;
								if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
									isHu = true;
								}
								// 没胡,加回来
								shouPaiArr[i] = paiNum;
								shouPaiArr[smallTwoJaioBiao] =smallTwoNum ;
								// 置空混牌数量
								shouPaiArr[hunPai - 1] = 0;
								if (isHu) {
									return true;
								}
							}
						}
					}else{
						//找大2的
						int bigTwoJaioBiao=i+2;
						if(needCheckList.contains(i) && needCheckList.contains(bigTwoJaioBiao)){//说明测过这个了
						}else{
							if(!needCheckList.contains(i)){
								needCheckList.add(i);
							}
							if(!needCheckList.contains(bigTwoJaioBiao)){
								needCheckList.add(bigTwoJaioBiao);
							}
							
							int bigTwoNum = shouPaiArr[bigTwoJaioBiao];
							if(nowHunNum+bigTwoNum>0){
								if(bigTwoNum>0){
									shouPaiArr[bigTwoJaioBiao]=bigTwoNum-1;
								}else{
									nowHunNum--;
								}
								
								shouPaiArr[i]=paiNum-1;
								shouPaiArr[hunPai-1]=nowHunNum;
								if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
									isHu = true;
								}
								// 没胡,加回来
								shouPaiArr[i] = paiNum;
								shouPaiArr[bigTwoJaioBiao] =bigTwoNum ;
								// 置空混牌数量
								shouPaiArr[hunPai - 1] = 0;
								if (isHu) {
									return true;
								}
							}
						}
						//说明上面不成立,还原现在混的数量
						nowHunNum=hunNum;
						//找到小2
						int smallTwoJaioBiao=i-2;
						if(needCheckList.contains(i) && needCheckList.contains(smallTwoJaioBiao)){//说明测过这个了
						}else{
							if(!needCheckList.contains(i)){
								needCheckList.add(i);
							}
							if(!needCheckList.contains(smallTwoJaioBiao)){
								needCheckList.add(smallTwoJaioBiao);
							}
							
							int smallTwoNum=shouPaiArr[smallTwoJaioBiao];
							if(nowHunNum+smallTwoNum>0){
								if(smallTwoNum>0){
									shouPaiArr[smallTwoJaioBiao]=smallTwoNum-1;
								}else{
									nowHunNum--;
								}
								
								shouPaiArr[i]=paiNum-1;
								shouPaiArr[hunPai-1]=nowHunNum;
								if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
									isHu = true;
								}
								// 没胡,加回来
								shouPaiArr[i] = paiNum;
								shouPaiArr[smallTwoJaioBiao] =smallTwoNum ;
								// 置空混牌数量
								shouPaiArr[hunPai - 1] = 0;
								if (isHu) {
									return true;
								}
							}
						}
						
					}
				}
			}
		} else {
			// 最后那张不能是红中
			if (dongZuoPai < 28) {// 是万饼条
				// 定义动作牌的数子大小
				int dongZuoPaiNum = dongZuoPai % 9;
				// 检测卡
				// 动作牌不能是1和9 ----1,9怎么卡？
				if (dongZuoPaiNum != 1 && dongZuoPaiNum != 0) {
					// 看比它小1
					int smallOneNum = shouPaiArr[dongZuoPai - 2];
					// 看比它大1
					// 看比它大1和小1的是否存在
					int bigOneNum = shouPaiArr[dongZuoPai];

					int smallOne = 0;
					int bigOne = 0;

					if (smallOneNum > 0) {
						smallOne = 1;
					}
					if (bigOneNum > 0) {
						bigOne = 1;
					}
					if (smallOne + bigOne + hunNum >= 2) {
						int needHunNum = 0;
						if (smallOne == 1) {
							shouPaiArr[dongZuoPai - 2] = smallOneNum - 1;
						} else {// 没有这个混牌代替
							needHunNum++;
						}
						if (bigOne == 1) {
							shouPaiArr[dongZuoPai] = bigOneNum - 1;
						} else {// 没有这个混牌代替
							needHunNum++;
						}
						// 给混牌添加数量
						shouPaiArr[hunPai - 1] = hunNum - needHunNum;
						if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
							isHu = true;
						}
						// 没胡,加回来
						shouPaiArr[dongZuoPai - 2] = smallOneNum;
						shouPaiArr[dongZuoPai] = bigOneNum;
						// 置空混牌数量
						shouPaiArr[hunPai - 1] = 0;
						if (isHu) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * 边胡 就是 12胡3 89胡7
	 * 
	 * @param shouPaiArr
	 * @param hunNum
	 * @param hunPai
	 * @param dongZuoPai
	 * @return
	 */
	public static Boolean checkBian(int[] shouPaiArr, int hunNum, Integer hunPai, Integer dongZuoPai) {
		boolean isHu = false;
		if(dongZuoPai==3 || dongZuoPai==7 || dongZuoPai.equals(hunPai) ){
			//防止俩东风，俩混，来个3条或者混检测不出来
			//首先检测手中是否有俩混，有的话，移除能否胡就是夹胡，否则走下面
			if(hunNum>=2){
				shouPaiArr[hunPai-1]=hunNum-2;
				if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
					isHu=true;
				}
				shouPaiArr[hunPai-1]=0;
				if(isHu){
					return true;
				}
			}
		}
		// 检测边 --动作牌是3或者7
		if (dongZuoPai == hunPai) {
			//存1和9
			List<Integer> needCheckList=new ArrayList<Integer>();
			//找到手中一张牌和大于此牌2的牌
			for (int i = 0; i < 27; i++) {
				int nowHunNum=hunNum;
				int paiNum = shouPaiArr[i];
				if(shouPaiArr[i]>0){
					int paiBig=i%9+1;
					if(paiBig==1 ){//1和2
						//找大1的
						int bigOneJaioBiao=i+1;
						if(needCheckList.contains(i)){//说明测过这个了
						}else{
							needCheckList.add(i);
							
							int bigOneNum = shouPaiArr[bigOneJaioBiao];
							if(nowHunNum+bigOneNum>0){
								if(bigOneNum>0){
									shouPaiArr[bigOneJaioBiao]=bigOneNum-1;
								}else{
									nowHunNum--;
								}
								//减去夹的这两张牌
								shouPaiArr[i]=paiNum-1;
								shouPaiArr[hunPai-1]=nowHunNum;
								if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
									isHu = true;
								}
								// 没胡,加回来
								shouPaiArr[i] = paiNum;
								shouPaiArr[bigOneJaioBiao] =bigOneNum ;
								// 置空混牌数量
								shouPaiArr[hunPai - 1] = 0;
								if (isHu) {
									return true;
								}
							}
						}
					}else if(paiBig==2){//8和9
						int smallOneJaioBiao=i-1;
						if(needCheckList.contains(smallOneJaioBiao)){//说明测过这个了
						}else{
							needCheckList.add(smallOneJaioBiao);
							int smallOneNum=shouPaiArr[smallOneJaioBiao];
							if(nowHunNum+smallOneNum>0){
								if(smallOneNum>0){
									shouPaiArr[smallOneJaioBiao]=smallOneNum-1;
								}else{
									nowHunNum--;
								}
								
								shouPaiArr[i]=paiNum-1;
								shouPaiArr[hunPai-1]=nowHunNum;
								if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
									isHu = true;
								}
								// 没胡,加回来
								shouPaiArr[i] = paiNum;
								shouPaiArr[smallOneJaioBiao] =smallOneNum ;
								// 置空混牌数量
								shouPaiArr[hunPai - 1] = 0;
								if (isHu) {
									return true;
								}
							}
						}
					}else if(paiBig==8){//8和9
						//找大1的
						int bigOneJaioBiao=i+1;
						if(needCheckList.contains(bigOneJaioBiao)){//说明测过这个了
						}else{
							needCheckList.add(bigOneJaioBiao);
							
							int bigOneNum = shouPaiArr[bigOneJaioBiao];
							if(nowHunNum+bigOneNum>0){
								if(bigOneNum>0){
									shouPaiArr[bigOneJaioBiao]=bigOneNum-1;
								}else{
									nowHunNum--;
								}
								//减去夹的这两张牌
								shouPaiArr[i]=paiNum-1;
								shouPaiArr[hunPai-1]=nowHunNum;
								if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
									isHu = true;
								}
								// 没胡,加回来
								shouPaiArr[i] = paiNum;
								shouPaiArr[bigOneJaioBiao] =bigOneNum ;
								// 置空混牌数量
								shouPaiArr[hunPai - 1] = 0;
								if (isHu) {
									return true;
								}
							}
						}
					}else if(paiBig==9){//8和9
						int smallOneJaioBiao=i-1;
						if(needCheckList.contains(i)){//说明测过这个了
						}else{
							needCheckList.add(i);
							int smallOneNum=shouPaiArr[smallOneJaioBiao];
							if(nowHunNum+smallOneNum>0){
								if(smallOneNum>0){
									shouPaiArr[smallOneJaioBiao]=smallOneNum-1;
								}else{
									nowHunNum--;
								}
								
								shouPaiArr[i]=paiNum-1;
								shouPaiArr[hunPai-1]=nowHunNum;
								if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
									isHu = true;
								}
								// 没胡,加回来
								shouPaiArr[i] = paiNum;
								shouPaiArr[smallOneJaioBiao] =smallOneNum ;
								// 置空混牌数量
								shouPaiArr[hunPai - 1] = 0;
								if (isHu) {
									return true;
								}
							}
						}
					}
				}
			}
			

		} else {
			if (dongZuoPai < 28) {
				int dongZuoPaiNum = dongZuoPai % 9;
				if (dongZuoPaiNum == 3) {
					// 查看比它小2的
					int smallTwoNum = shouPaiArr[dongZuoPai - 3];
					// 查看比它小1的
					int smallOneNum = shouPaiArr[dongZuoPai - 2];
					int smallTwo = 0;
					int smallOne = 0;
					if (smallTwoNum > 0) {
						smallTwo = 1;
					}
					if (smallOneNum > 0) {
						smallOne = 1;
					}
					if (smallTwo + smallOne + hunNum >= 2) {
						int needHunNum = 0;
						if (smallTwo == 1) {
							shouPaiArr[dongZuoPai - 3] = smallTwoNum - 1;
						} else {// 没有这个混牌代替
							needHunNum++;
						}
						if (smallOne == 1) {
							shouPaiArr[dongZuoPai - 2] = smallOneNum - 1;
						} else {// 没有这个混牌代替
							needHunNum++;
						}
						// 给混牌添加数量
						shouPaiArr[hunPai - 1] = hunNum - needHunNum;
						if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
							isHu = true;
						}
						// 没胡,加回来
						shouPaiArr[dongZuoPai - 3] = smallTwoNum;
						shouPaiArr[dongZuoPai - 2] = smallOneNum;
						// 置空混牌数量
						shouPaiArr[hunPai - 1] = 0;
						if (isHu) {
							return true;
						}
					}
				} else if (dongZuoPai == 7) {
					// 查看比它大1的
					int bigOneNum = shouPaiArr[dongZuoPai];
					// 查看比它大2的
					int bigTwoNum = shouPaiArr[dongZuoPai + 1];
					int bigOne = 0;
					int bigTwo = 0;
					if (bigOneNum > 0) {
						bigOne = 1;
					}
					if (bigTwoNum > 0) {
						bigTwo = 1;
					}
					if (bigOne + bigTwo + hunNum >= 2) {
						int needHunNum = 0;
						if (bigOne == 1) {
							shouPaiArr[dongZuoPai] = bigOneNum - 1;
						} else {// 没有这个混牌代替
							needHunNum++;
						}
						if (bigTwo == 1) {
							shouPaiArr[dongZuoPai + 1] = bigTwoNum - 1;
						} else {// 没有这个混牌代替
							needHunNum++;
						}
						// 给混牌添加数量
						shouPaiArr[hunPai - 1] = hunNum - needHunNum;
						if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
							isHu = true;
						}
						// 没胡,加回来
						shouPaiArr[dongZuoPai] = bigOneNum;
						shouPaiArr[dongZuoPai + 1] = bigTwoNum;
						// 置空混牌数量
						shouPaiArr[hunPai - 1] = 0;
						if (isHu) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * @param shouPaiArr
	 * @param hunNum
	 * @param hunPai
	 * @param dongZuoPai
	 * @return
	 */
	public static Boolean checkDiao(int[] shouPaiArr, int hunNum, Integer hunPai, Integer dongZuoPai) {
		boolean isHu = false;
		//检测单吊手中的一个混排
		if(hunNum>0){
			shouPaiArr[hunPai-1]=hunNum-1;
			if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
				isHu = true;
			}
			shouPaiArr[hunPai-1]=0;
			if(isHu){
				return true;
			}
		}
		if (dongZuoPai.equals(hunPai)) {
			for (int i = 0; i < 34; i++) {
				shouPaiArr[hunPai-1]=hunNum;
				int paiNum = shouPaiArr[i];
				if(paiNum>0){
					shouPaiArr[i]=paiNum-1;
					if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
						isHu = true;
					}
					shouPaiArr[i]=paiNum;
					shouPaiArr[hunPai-1]=0;
					if(isHu){
						return true;
					}
				}
			}
		} else {
			// 检测吊
			int paiNum = shouPaiArr[dongZuoPai - 1];
			if ((paiNum + shouPaiArr[dongZuoPai - 1]) > 0) {
				int needHum = 0;
				if (paiNum > 0) {
					shouPaiArr[dongZuoPai - 1] = paiNum - 1;
				} else {
					needHum++;
				}
				if (hunPai != 35) {
					shouPaiArr[hunPai - 1] = hunNum - needHum;
				}
				if (Hulib.getInstance().get_hu_info(shouPaiArr, 34, hunPai - 1)) {
					isHu = true;
				}
				// 数据还原
				if (needHum == 1) {
					shouPaiArr[hunPai - 1] = 0;
				} else {
					shouPaiArr[hunPai - 1] = 0;
					shouPaiArr[dongZuoPai - 1] = paiNum;
				}
				if (isHu) {
					return true;
				}
			}
		}
		return false;
	}

}
