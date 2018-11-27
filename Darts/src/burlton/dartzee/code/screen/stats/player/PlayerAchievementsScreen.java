package burlton.dartzee.code.screen.stats.player;

import burlton.dartzee.code.achievements.AchievementConstants;
import burlton.dartzee.code.bean.AchievementMedal;
import burlton.dartzee.code.db.AchievementEntity;
import burlton.dartzee.code.db.PlayerEntity;
import burlton.dartzee.code.screen.EmbeddedScreen;
import burlton.dartzee.code.screen.ScreenCache;
import javafx.scene.paint.Color;

import javax.swing.*;
import java.awt.*;

public final class PlayerAchievementsScreen extends EmbeddedScreen
{
	private PlayerEntity player = null;
	
	
	public PlayerAchievementsScreen() 
	{	
		JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
		add(tabbedPane, BorderLayout.CENTER);
		
		
		tabbedPane.addTab("General", null, panelGeneral, null);
		
		panelGeneral.add(gray);
		panelGeneral.add(red);
		panelGeneral.add(orange);
		panelGeneral.add(yellow);
		panelGeneral.add(green);
		panelGeneral.add(cyan);
		panelGeneral.add(hotpink);
		
		
		//FlowLayout flowLayout = (FlowLayout) panelX01.getLayout();
		//flowLayout.setVgap(20);
		//flowLayout.setHgap(20);
		//flowLayout.setAlignment(FlowLayout.LEFT);
		//tabbedPane.addTab("X01", null, panelX01, null);
			
		//panelX01.add(lblNewLabel);
	}
	
	private final JPanel panelGeneral = new JPanel();
	private final AchievementMedal gray = new AchievementMedal(15, Color.GRAY);
	private final AchievementMedal red = new AchievementMedal(45, Color.RED);
	private final AchievementMedal orange = new AchievementMedal(85, Color.ORANGE);
	private final AchievementMedal yellow = new AchievementMedal(180, Color.YELLOW);
	private final AchievementMedal green = new AchievementMedal(210, Color.LIGHTGREEN);
	private final AchievementMedal cyan = new AchievementMedal(266, Color.CYAN);
	private final AchievementMedal hotpink = new AchievementMedal(360, Color.DEEPPINK);
	private final JLabel lblNewLabel = new JLabel("");
	
	@Override
	public String getScreenName()
	{
		return "Achievements - " + player.getName();
	}

	@Override
	public void initialise()
	{
		long playerId = player.getRowId();
		
		AchievementEntity achievement = AchievementEntity.retrieveAchievement(AchievementConstants.ACHIEVEMENT_REF_X01_BEST_FINISH, playerId);
		if (achievement != null)
		{
			lblNewLabel.setText("Best Finish: " + achievement.getAchievementCounter() + ", Game #" + achievement.getGameIdEarned());
		}
		else
		{
			lblNewLabel.setText("");
		}
	}
	
	/*@Override
	public void postInit()
	{
		HandyArrayList<AchievementMedal> medals = ComponentUtil.getAllChildComponentsForType(this, AchievementMedal.class);
		for (AchievementMedal medal : medals)
		{
			medal.animateProgressBar();
		}
	}*/
	
	@Override
	public EmbeddedScreen getBackTarget()
	{
		return ScreenCache.getPlayerManagementScreen();
	}

	public void setPlayer(PlayerEntity player)
	{
		this.player = player;
	}
}
