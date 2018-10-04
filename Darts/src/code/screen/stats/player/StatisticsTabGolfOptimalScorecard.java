package code.screen.stats.player;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JPanel;

import code.object.Dart;
import code.screen.game.DartsScorerGolf;
import code.stats.GameWrapper;
import object.HandyArrayList;
import object.HashMapList;

/**
 * Cherry-picks your best performance ever for each hole and assembles it into an 'ideal' scorecard.
 * Just for fun!
 */
public class StatisticsTabGolfOptimalScorecard extends AbstractStatisticsTab
{
	public StatisticsTabGolfOptimalScorecard() 
	{
		setLayout(new GridLayout(0, 2, 0, 0));
		add(panelMine);
		panelMine.setLayout(new BorderLayout(0, 0));
		panelOther.setLayout(new BorderLayout(0, 0));
		
		panelMine.add(panelMyScorecard);
		panelMyScorecard.setLayout(new BorderLayout(0, 0));
		add(panelOther);
		panelOther.add(panelOtherScorecard);
		panelOtherScorecard.setLayout(new BorderLayout(0, 0));
		
		DartsScorerGolf scorer = new DartsScorerGolf();
		scorer.init(null, null);
		panelMyScorecard.add(scorer, BorderLayout.CENTER);
	}
	
	private final JPanel panelMine = new JPanel();
	private final JPanel panelMyScorecard = new JPanel();
	private final JPanel panelOther = new JPanel();
	private final JPanel panelOtherScorecard = new JPanel();

	@Override
	public void populateStats()
	{
		setOtherComponentVisibility(this, panelOther);
		
		populateStats(filteredGames, panelMyScorecard, null);
		if (includeOtherComparison())
		{
			populateStats(filteredGamesOther, panelOtherScorecard, Color.RED);
		}
	}
	private void populateStats(HandyArrayList<GameWrapper> filteredGames, JPanel panel, Color color)
	{
		HashMapList<Integer, Dart> hmHoleToBestDarts = new HashMapList<>();
		HashMap<Integer, Long> hmHoleToBestGameId = new HashMap<>();
		for (GameWrapper game : filteredGames)
		{
			game.populateOptimalScorecardMaps(hmHoleToBestDarts, hmHoleToBestGameId);
		}
		
		DartsScorerGolf scorer = new DartsScorerGolf();
		if (color != null)
		{
			scorer.setTableForeground(Color.RED);
		}
		
		scorer.setShowGameId(true);
		scorer.init(null, null);
		
		for (int i=1; i<=18; i++)
		{
			ArrayList<Dart> darts = hmHoleToBestDarts.get(i);
			if (darts != null)
			{
				long gameId = hmHoleToBestGameId.get(i);
				scorer.addDarts(darts, gameId);
			}
		}
		
		panel.removeAll();
		panel.add(scorer, BorderLayout.CENTER);
		panel.revalidate();
		panel.repaint();
	}

}
