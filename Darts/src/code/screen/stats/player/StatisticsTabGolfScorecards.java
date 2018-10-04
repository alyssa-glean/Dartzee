package code.screen.stats.player;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import bean.ComboBoxItem;
import bean.RowSelectionListener;
import bean.ScrollTable;
import code.bean.ScrollTableDartsGame;
import code.screen.game.DartsScorerGolf;
import code.stats.GameWrapper;
import object.HandyArrayList;
import util.TableUtil.DefaultModel;

public class StatisticsTabGolfScorecards extends AbstractStatisticsTab
										 implements ActionListener, RowSelectionListener
{
	private int mode = -1;
	
	public StatisticsTabGolfScorecards() 
	{
		setLayout(new BorderLayout(0, 0));
		
		JPanel panelMode = new JPanel();
		add(panelMode, BorderLayout.NORTH);
		panelMode.setLayout(new FlowLayout(FlowLayout.LEADING, 5, 5));
		
		panelMode.add(lblMode);
		
		panelMode.add(comboBoxMode);
		
		comboBoxMode.addActionListener(this);
		
		add(panelCenter, BorderLayout.CENTER);
		panelCenter.setLayout(new GridLayout(0, 2, 0, 0));
		
		panelCenter.add(panelMine);
		panelMine.setLayout(new GridLayout(0, 2, 0, 0));
		
		panelMine.add(scrollTableMine);
		
		panelMine.add(panelMyScorecard);
		panelMyScorecard.setLayout(new BorderLayout(0, 0));
		
		panelCenter.add(panelOther);
		panelOther.setLayout(new GridLayout(0, 2, 0, 0));
		panelOther.add(scrollTableOther);
		
		scrollTableOther.setTableForeground(Color.RED);
		
		panelOther.add(panelOtherScorecard);
		panelOtherScorecard.setLayout(new BorderLayout(0, 0));
		
		scrollTableMine.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollTableOther.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		scrollTableMine.addRowSelectionListener(this);
		scrollTableOther.addRowSelectionListener(this);
	}
	private final JComboBox<ComboBoxItem<Integer>> comboBoxMode = new JComboBox<>();
	private final JPanel panelCenter = new JPanel();
	private final JLabel lblMode = new JLabel("Mode");
	private final JPanel panelMine = new JPanel();
	private final JPanel panelOther = new JPanel();
	private final ScrollTableDartsGame scrollTableMine = new ScrollTableDartsGame();
	private final JPanel panelMyScorecard = new JPanel();
	private final ScrollTableDartsGame scrollTableOther = new ScrollTableDartsGame();
	private final JPanel panelOtherScorecard = new JPanel();

	@Override
	public void populateStats()
	{
		populateStats(true);
	}
	private void populateStats(boolean rebuildComboBox)
	{
		if (rebuildComboBox)
		{
			initialiseComboBoxModel();
		}
		
		//Hide or show the 'other' panel depending on whether there's a comparison
		setOtherComponentVisibility(panelCenter, panelOther);
		
		//Update the mode based on what's selected in the combo box
		int ix = comboBoxMode.getSelectedIndex();
		ComboBoxItem<Integer> item = comboBoxMode.getItemAt(ix);
		mode = item.getHiddenData();
		
		//And now populate the table(s)
		populateTable(filteredGames, scrollTableMine);
		if (includeOtherComparison())
		{
			populateTable(filteredGamesOther, scrollTableOther);
		}
	}
	private void initialiseComboBoxModel()
	{
		DefaultComboBoxModel<ComboBoxItem<Integer>> model = new DefaultComboBoxModel<>();
		addMode("Front 9", GameWrapper.MODE_FRONT_9, model);
		addMode("Back 9", GameWrapper.MODE_BACK_9, model);
		addMode("Full 18", GameWrapper.MODE_FULL_18, model);
		
		if (model.getSize() == 0)
		{
			model.addElement(new ComboBoxItem<>(GameWrapper.MODE_FULL_18, "N/A"));
		}
		
		comboBoxMode.setModel(model);
	}
	private void addMode(String modeDesc, int mode, DefaultComboBoxModel<ComboBoxItem<Integer>> model)
	{
		HandyArrayList<GameWrapper> validGames = filteredGames.createFilteredCopy(g -> g.getRoundScore(mode) > -1);
		if (validGames.isEmpty())
		{
			return;
		}
		
		ComboBoxItem<Integer> item = new ComboBoxItem<>(mode, modeDesc);	
		model.addElement(item);
	}
	
	
	private void populateTable(HandyArrayList<GameWrapper> filteredGames, ScrollTableDartsGame scrollTable)
	{
		//Filter out the -1's - these are games that haven't gone on long enough to have all the data
		HandyArrayList<GameWrapper> validGames = filteredGames.createFilteredCopy(g -> g.getRoundScore(mode) > -1);
		
		//Populate the table from the wrappers
		DefaultModel model = new DefaultModel();
		model.addColumn("Game");
		model.addColumn("Score");
		model.addColumn("!GameObject");
		
		for (GameWrapper game : validGames)
		{
			long gameId = game.getGameId();
			int score = game.getRoundScore(mode);
			
			Object[] row = {gameId, score, game};
			model.addRow(row);
		}
		
		scrollTable.setModel(model);
		scrollTable.removeColumn(2);
		scrollTable.sortBy(1, false);
		
		//Select a row so the scorecard automatically populates
		if (!validGames.isEmpty())
		{
			scrollTable.selectFirstRow();
		}
	}
	
	private void displayScorecard(GameWrapper game, JPanel scorecardPanel)
	{
		DartsScorerGolf scorer = new DartsScorerGolf();
		scorer.init(null, game.getGameParams());
		if (mode == GameWrapper.MODE_BACK_9)
		{
			scorer.setFudgeFactor(9);
		}
		
		game.populateScorer(scorer, mode);
		
		scorecardPanel.removeAll();
		scorecardPanel.add(scorer, BorderLayout.CENTER);
		scorecardPanel.revalidate();
		scorecardPanel.repaint();
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		populateStats(false);
	}
	
	@Override
	public void selectionChanged(ScrollTable src)
	{
		int row = src.getSelectedModelRow();
		if (row == -1)
		{
			return;
		}
		
		GameWrapper game = (GameWrapper)src.getValueAt(row, 2);
		if (src == scrollTableMine)
		{
			displayScorecard(game, panelMyScorecard);
		}
		else
		{
			displayScorecard(game, panelOtherScorecard);
		}
		
	}
}
