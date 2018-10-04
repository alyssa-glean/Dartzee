package code.screen;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import code.bean.PlayerAvatar;
import code.db.GameEntity;
import code.db.PlayerEntity;
import code.screen.ai.AIConfigurationDialog;
import code.screen.ai.AISimulationSetup;
import net.miginfocom.swing.MigLayout;
import util.DialogUtil;

public class PlayerManagementPanel extends JPanel
								   implements ActionListener
{
	private PlayerEntity player = null;
	
	public PlayerManagementPanel() 
	{
		setLayout(new BorderLayout(0, 0));
		
		JPanel panelOptions = new JPanel();
		add(panelOptions, BorderLayout.SOUTH);
		btnEdit.setFont(new Font("Tahoma", Font.PLAIN, 16));
		panelOptions.add(btnEdit);
		btnRunSimulation.setFont(new Font("Tahoma", Font.PLAIN, 16));
		
		panelOptions.add(btnRunSimulation);
		btnDelete.setFont(new Font("Tahoma", Font.PLAIN, 16));
		panelOptions.add(btnDelete);
		
		btnEdit.addActionListener(this);
		btnRunSimulation.addActionListener(this);
		btnDelete.addActionListener(this);
		
		add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));
		lblPlayerName.setPreferredSize(new Dimension(0, 25));
		panel.add(lblPlayerName, BorderLayout.NORTH);
		lblPlayerName.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblPlayerName.setHorizontalAlignment(SwingConstants.CENTER);
		
		panel.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new MigLayout("", "[grow][]", "[][][][][]"));
		panel_1.add(avatar, "cell 0 0 3 1,alignx center");
		avatar.setPreferredSize(new Dimension(150, 150));
		panel_1.add(panelX01, "cell 0 1 2 1,grow");
		panel_1.add(panelGolf, "cell 0 2 2 1,grow");
		panel_1.add(panelClock, "cell 0 3 2 1,grow");
	}
	
	private final JButton btnEdit = new JButton("Edit");
	private final JButton btnDelete = new JButton("Delete");
	private final JLabel lblPlayerName = new JLabel("");
	private final JPanel panel = new JPanel();
	private final PlayerAvatar avatar = new PlayerAvatar();
	private final JPanel panel_1 = new JPanel();
	private final PlayerSummaryPanel panelX01 = new PlayerSummaryPanel(GameEntity.GAME_TYPE_X01);
	private final PlayerSummaryPanel panelGolf = new PlayerSummaryPanel(GameEntity.GAME_TYPE_GOLF);
	private final PlayerSummaryPanel panelClock = new PlayerSummaryPanel(GameEntity.GAME_TYPE_ROUND_THE_CLOCK);
	private final JButton btnRunSimulation = new JButton("Run Simulation");
	
	public void init(PlayerEntity player)
	{
		this.player = player;
		
		lblPlayerName.setText(player.getName());
		
		//Only show this for AIs
		btnEdit.setVisible(player.isAi());
		btnRunSimulation.setVisible(player.isAi());
		
		btnDelete.setEnabled(true);
		
		avatar.setVisible(true);
		panelX01.init(player);
		panelGolf.init(player);
		panelClock.init(player);
		avatar.init(player, true);
	}
	
	public void clear()
	{
		this.player = null;
		lblPlayerName.setText("");
		btnEdit.setVisible(false);
		btnRunSimulation.setVisible(false);
		avatar.setVisible(false);
		panelX01.setVisible(false);
		panelGolf.setVisible(false);
		panelClock.setVisible(false);
		
		btnDelete.setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		Object source = arg0.getSource();
		if (source == btnEdit)
		{
			AIConfigurationDialog.amendPlayer(player);
		}
		else if (source == btnDelete)
		{
			confirmAndDeletePlayer();
		}
		else if (source == btnRunSimulation)
		{
			AISimulationSetup dlg = new AISimulationSetup(player);
			dlg.setVisible(true);
		}
	}
	
	private void confirmAndDeletePlayer()
	{
		int option = DialogUtil.showQuestion("Are you sure you want to delete " + player.getName() + "?", false);
		if (option == JOptionPane.YES_OPTION)
		{
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			player.setDtDeleted(timestamp);
			player.saveToDatabase();
			
			//Re-init the screen so it updates
			PlayerManagementScreen screen = ScreenCache.getPlayerManagementScreen();
			screen.init();
		}
	}
}
