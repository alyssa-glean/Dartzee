package code.screen.stats.player;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import code.db.GameEntity;
import code.db.PlayerEntity;
import code.object.Dart;
import code.screen.EmbeddedScreen;
import code.screen.PlayerSelectDialog;
import code.screen.ScreenCache;
import code.stats.GameWrapper;
import code.utils.DatabaseUtil;
import object.HandyArrayList;
import object.SuperHashMap;
import util.ComponentUtil;
import util.Debug;

public class PlayerStatisticsScreen extends EmbeddedScreen
{
	private SuperHashMap<Long, GameWrapper> hmGameIdToWrapper = new SuperHashMap<>();
	private SuperHashMap<Long, GameWrapper> hmGameIdToWrapperOther = new SuperHashMap<>();
	private HandyArrayList<GameWrapper> filteredGames = new HandyArrayList<>();
	private HandyArrayList<GameWrapper> filteredGamesOther = new HandyArrayList<>();
	
	private int gameType = -1;
	private PlayerEntity player = null;
	
	public PlayerStatisticsScreen()
	{
		super();
		
		add(filterPanels, BorderLayout.NORTH);
		add(tabbedPane, BorderLayout.CENTER);
		
		filterPanels.add(filterPanel);
		filterPanels.add(filterPanelOther);
		filterPanels.add(btnAdd);
		
		btnAdd.addActionListener(this);
	}
	
	//Components
	private final JPanel filterPanels = new JPanel();
	private final PlayerStatisticsFilterPanel filterPanel = new PlayerStatisticsFilterPanel();
	private final PlayerStatisticsFilterPanel filterPanelOther = new PlayerStatisticsFilterPanel();
	protected final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
	private final JButton btnAdd = new JButton("Add Comparison");
	
	//X01 tabs
	protected final StatisticsTabFinishBreakdown tabFinishing = new StatisticsTabFinishBreakdown();
	protected final StatisticsTabX01CheckoutPercent tabCheckoutPercent = new StatisticsTabX01CheckoutPercent();
	protected final StatisticsTabTopFinishes tabTopFinishes = new StatisticsTabTopFinishes();
	protected final StatisticsTabThreeDartAverage tabThreeDartAverage = new StatisticsTabThreeDartAverage();
	protected final StatisticsTabTotalScore tabTotalDarts = new StatisticsTabTotalScore("Total Darts", 200);
	protected final StatisticsTabThreeDartScores tabThreeDartScores = new StatisticsTabThreeDartScores();
	
	//Golf tabs
	protected final StatisticsTabGolfHoleBreakdown tabHoleBreakdown = new StatisticsTabGolfHoleBreakdown();
	protected final StatisticsTabTotalScore tabAllScores = new StatisticsTabTotalScore("Total Shots", 90);
	protected final StatisticsTabGolfScorecards tabBestRounds = new StatisticsTabGolfScorecards();
	protected final StatisticsTabGolfOptimalScorecard tabOptimalScorecard = new StatisticsTabGolfOptimalScorecard();
	
	//Round the Clock tabs
	protected final StatisticsTabTotalScore tabTotalClockDarts = new StatisticsTabTotalScore("Total Darts", 500);
	
	@Override
	public String getScreenName()
	{
		return "Statistics for " + player;
	}

	@Override
	public void init()
	{
		filterPanel.init(player, gameType, false);
		filterPanelOther.setVisible(false);
		btnAdd.setVisible(true);
		
		hmGameIdToWrapper = retrieveGameData(player.getRowId());
		hmGameIdToWrapperOther.clear();
		
		resetTabs();
		buildTabs();
	}
	
	/**
	 * Called when popping this up in a dialog after simulating games from the player amendment dialog (for AIs)
	 */
	public void initFake(SuperHashMap<Long, GameWrapper> hmGameIdToWrapper)
	{
		filterPanel.init(player, gameType, false);
		filterPanelOther.setVisible(false);
		btnAdd.setVisible(false);
		btnBack.setVisible(false);
		
		this.hmGameIdToWrapper = hmGameIdToWrapper;
		
		resetTabs();
		buildTabs();
	}
	
	private void resetTabs()
	{
		tabbedPane.removeAll();
		
		if (gameType == GameEntity.GAME_TYPE_X01)
		{
			tabbedPane.addTab("Finish Breakdown", null, tabFinishing, null);
			tabbedPane.addTab("Checkout %", null, tabCheckoutPercent, null);
			tabbedPane.addTab("Top Finishes", null, tabTopFinishes, null);
			tabbedPane.addTab("Dart Average", null, tabThreeDartAverage, null);
			tabbedPane.addTab("Total Darts", null, tabTotalDarts, null);
			tabbedPane.addTab("Three Dart Scores", null, tabThreeDartScores, null);
		}
		else if (gameType == GameEntity.GAME_TYPE_GOLF)
		{
			tabbedPane.addTab("Hole Breakdown", null, tabHoleBreakdown, null);
			tabbedPane.addTab("Scorecards", null, tabBestRounds, null);
			tabbedPane.addTab("Optimal Scorecard", null, tabOptimalScorecard, null);
			tabbedPane.addTab("All Scores", null, tabAllScores, null);
		}
		else if (gameType == GameEntity.GAME_TYPE_ROUND_THE_CLOCK)
		{
			tabbedPane.addTab("Total Darts", null, tabTotalClockDarts, null);
		}
	}
	
	private void addComparison()
	{
		PlayerEntity player = PlayerSelectDialog.selectPlayer();
		if (player == null)
		{
			//Cancelled
			return;
		}
		
		filterPanelOther.init(player, gameType, true);
		filterPanelOther.setVisible(true);
		btnAdd.setVisible(false);
		
		hmGameIdToWrapperOther = retrieveGameData(player.getRowId());
		buildTabs();
	}
	
	public void removeComparison()
	{
		filterPanelOther.setVisible(false);
		btnAdd.setVisible(true);
		hmGameIdToWrapperOther = new SuperHashMap<>();
		
		buildTabs();
	}
	
	private SuperHashMap<Long, GameWrapper> retrieveGameData(long playerId)
	{
		SuperHashMap<Long, GameWrapper> hm = new SuperHashMap<>();
		
		StringBuilder sb = new StringBuilder();
		sb.append(" SELECT g.RowId, g.GameParams, g.DtCreation, g.DtFinish,");
		sb.append(" gp.FinalScore, ");
		sb.append(" rnd.RoundNumber,");
		sb.append(" drt.Ordinal, drt.Score, drt.Multiplier, drt.StartingScore, drt.SegmentType");
		sb.append(" FROM Dart drt, Round rnd, Participant gp, Game g");
		sb.append(" WHERE drt.RoundId = rnd.RowId");
		sb.append(" AND rnd.ParticipantId = gp.RowId");
		sb.append(" AND gp.GameId = g.RowId");
		sb.append(" AND gp.PlayerId = ");
		sb.append(playerId);
		sb.append(" AND g.GameType = ");
		sb.append(gameType);
		
		try (ResultSet rs = DatabaseUtil.executeQuery(sb))
		{
			while (rs.next())
			{
				long gameId = rs.getLong("RowId");
				String gameParams = rs.getString("GameParams");
				Timestamp dtStart = rs.getTimestamp("DtCreation");
				Timestamp dtFinish = rs.getTimestamp("DtFinish");
				int numberOfDarts = rs.getInt("FinalScore");
				int roundNumber = rs.getInt("RoundNumber");
				int ordinal = rs.getInt("Ordinal");
				int score = rs.getInt("Score");
				int multiplier = rs.getInt("Multiplier");
				int startingScore = rs.getInt("StartingScore");
				int segmentType = rs.getInt("SegmentType");
				
				GameWrapper wrapper = hm.get(gameId);
				if (wrapper == null)
				{
					wrapper = new GameWrapper(gameId, gameParams, dtStart, dtFinish, numberOfDarts);
					hm.put(gameId, wrapper);
				}
				
				Dart dart = new Dart(score, multiplier);
				dart.setOrdinal(ordinal);
				dart.setStartingScore(startingScore);
				dart.setSegmentType(segmentType);
				wrapper.addDart(roundNumber, dart);
			}
		}
		catch (SQLException sqle)
		{
			Debug.logSqlException(sb.toString(), sqle);
		}
		
		return hm;
	}
	
	public void buildTabs()
	{
		filteredGames = populateFilteredGames(hmGameIdToWrapper, filterPanel);
		filteredGamesOther = populateFilteredGames(hmGameIdToWrapperOther, filterPanelOther);
		
		//Update the tabs
		ArrayList<AbstractStatisticsTab> tabs = ComponentUtil.getAllChildComponentsForType(this, AbstractStatisticsTab.class);
		for (AbstractStatisticsTab tab : tabs)
		{
			tab.setFilteredGames(filteredGames, filteredGamesOther);
			tab.populateStats();
		}
	}
	
	private HandyArrayList<GameWrapper> populateFilteredGames(SuperHashMap<Long, GameWrapper> hmGameIdToWrapper, 
	  PlayerStatisticsFilterPanel filterPanel)
	{
		HandyArrayList<GameWrapper> allGames = hmGameIdToWrapper.getValuesAsVector();
		if (!filterPanel.isVisible())
		{
			return allGames;
		}
		
		HandyArrayList<GameWrapper> filteredGames = allGames.createFilteredCopy(g -> filterPanel.includeGame(g));
		filterPanel.update(filteredGames);
		return filteredGames;
	}
	
	public void setVariables(int gameType, PlayerEntity player)
	{
		this.gameType = gameType;
		this.player = player;
	}
	
	@Override
	public EmbeddedScreen getBackTarget()
	{
		return ScreenCache.getPlayerManagementScreen();
	}
	
	
	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		if (arg0.getSource() == btnAdd)
		{
			addComparison();
		}
		else
		{
			super.actionPerformed(arg0);
		}
	}
}
