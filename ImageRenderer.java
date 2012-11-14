package IBDML;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import IBDML.Fly_Data_Extraction.iconStock;

class ImageRenderer extends DefaultTableCellRenderer 
{
	private static final long serialVersionUID = 1L;

	public ImageRenderer()
	{

	} 
	
	JLabel lbl = new JLabel();

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,boolean hasFocus, int row, int column) 
	{
		//lbl.setText((String) value);
		lbl.setIcon(((iconStock) value).icon);
		return lbl;
	}
}
