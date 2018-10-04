package code.utils;

import code.ai.AbstractDartsModel;
import code.object.Dart;

public abstract class X01Util
{
	public static boolean isBust(int score, Dart lastDart)
	{
		return score < 0
		  || score == 1
		  || (score == 0 && !lastDart.isDouble());
	}
	
	/**
	 * Apply the Mercy Rule if:
	 *  - It has been enabled for this AI
	 *  - The starting score was odd and < the threshold (configurable per AI)
	 *  - The current score is even, meaing we have bailed ourselves out in some way
	 */
	public static boolean shouldStopForMercyRule(AbstractDartsModel model, int startingScore, int currentScore)
	{
		int mercyThreshold = model.getMercyThreshold();
		if (mercyThreshold == -1)
		{
			return false;
		}
		
		return startingScore < mercyThreshold
		  && startingScore % 2 != 0
		  && currentScore % 2 == 0;
	}
	
	/**
	 * 50, 40, 38, 36, 34, ... , 8, 4, 2
	 */
	public static boolean isCheckoutDart(Dart drt)
	{
		int startingScore = drt.getStartingScore();
		
		//Special case for bullseye
		if (startingScore == 50)
		{
			return true;
		}
		
		return startingScore % 2 == 0 //Even
		  && startingScore <= 40;
	}
}
