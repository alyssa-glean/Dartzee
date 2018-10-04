package code.screen.stats.player;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.SwingConstants;

import code.bean.ScrollTableDartsGame;
import code.object.Dart;
import code.stats.GameWrapper;
import object.HandyArrayList;
import util.StringUtil;
import util.TableUtil;
import util.TableUtil.SimpleRenderer;

public class StatisticsTabTopFinishes extends AbstractStatisticsTab
{
	private static final int MAX_FINISHES_TO_SHOW = 25;
	
	public StatisticsTabTopFinishes()
	{
		super();
		
		setLayout(new GridLayout(0, 2, 0, 0));
		
		add(tableTopFinishesMine);
		add(tableTopFinishesOther);
		
		tableTopFinishesOther.setTableForeground(Color.RED);
	}
	
	private final ScrollTableDartsGame tableTopFinishesMine = new ScrollTableDartsGame();
	private final ScrollTableDartsGame tableTopFinishesOther = new ScrollTableDartsGame();
	
	@Override
	public void populateStats()
	{
		setOtherComponentVisibility(this, tableTopFinishesOther);
		
		buildTopFinishesTable(filteredGames, tableTopFinishesMine);
		if (includeOtherComparison())
		{
			buildTopFinishesTable(filteredGamesOther, tableTopFinishesOther);
		}
	}
	
	private void buildTopFinishesTable(HandyArrayList<GameWrapper> games, ScrollTableDartsGame table)
	{
		TableUtil.DefaultModel model = new TableUtil.DefaultModel();
		model.addColumn("Finish");
		model.addColumn("Darts");
		model.addColumn("Game");
		
		//Sort by checkout total. 
		games.sort((GameWrapper g1, GameWrapper g2) 
							-> Integer.compare(g2.getCheckoutTotal(), g1.getCheckoutTotal()));
		
		int listSize = Math.min(MAX_FINISHES_TO_SHOW, games.size());
		for (int i=0; i<listSize; i++)
		{
			GameWrapper game = games.get(i);
			if (!game.isFinished())
			{
				continue;
			}
			
			long gameId = game.getGameId();
			int total = game.getCheckoutTotal();
			
			ArrayList<Dart> darts = game.getDartsForFinalRound();
			String dartStr = StringUtil.toDelims(darts, ", ");
			
			Object[] row = {total, dartStr, gameId};
			model.addRow(row);
		}
		
		table.setModel(model);
		table.setRenderer(0, new SimpleRenderer(SwingConstants.LEFT, null));
		table.sortBy(0, true);
	}
}
