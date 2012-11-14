/*		31/01/2012
 * 		Arnaud Polette 	: po.arnaud@gmail.com
 * 		Lionel Mille	: lionel.fils@gmail.com
 * 
 * 	Ce plug in pour ImageJ a pour but d'analyser et d'extraire des données 
 * au seins d'ailes de mouches. 
 * */

/*
 * Attention on parlera souvent de nappe dans la description des méthodes 
 * Cette nappe correspond a l'image initiale que l'on aurait élevé pour
 * la voir sous forme de volume.
 * Pour un pixel en coordonée (x,y) son élévation correspond a son
 * niveau de gris dans l'une des trois composante du modèle HLS
 * */

package IBDML;
import java.awt.*;
import java.awt.event.*;

import java.io.*;

import ij.*;
import ij.gui.*;

import ij.plugin.frame.*;

import ij.process.*;

import javax.swing.ImageIcon;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;


public class Fly_Data_Extraction extends PlugInFrame
{

	private static final long serialVersionUID = 1L;
	static Frame instance;
	    
	static boolean miniMod = false;
	static int nCut = 10;
	
	/*
	 * Structure dans laquelle on enregistrera les informations de l'image */
	class dataSaveImage 
	{ 
		public dataSaveImage() {icon = new iconStock(); name = new String();}
		public iconStock icon;
		public long max;
		public Point posMax;
		public float Volume1;
		public Roi roiTache[];
		public int surface[];
		public int barX;
		public int barY;
		public String name;
	}
	
	/*
	 * Miniature de l'image */
	public class iconStock 
	{ 
		public iconStock() {icon = new ImageIcon();}
		public ImageIcon icon;
		public int ID;
	}
	
	JTable table;
	JButton button;
	File[] fl;
	
	dataSaveImage[] myImgs;
	
	JPanel panel;
	
	public Fly_Data_Extraction() 
	{
		super("Wing_Analyser");
	}
	
	public void run(String arg) 
	{
		int i;
		instance = this;
		
		/* On ouvre plusieurs images */
		javax.swing.JFileChooser jfc = new javax.swing.JFileChooser(ij.io.OpenDialog.getDefaultDirectory());
		jfc.setMultiSelectionEnabled(true);
		jfc.showOpenDialog(null);
		ij.io.OpenDialog.setDefaultDirectory(jfc.getCurrentDirectory().getPath());
		fl = jfc.getSelectedFiles();
		/*   **   **   */

	    ij.io.Opener o = new ij.io.Opener();

	    myImgs = new dataSaveImage[fl.length];
	    for(i=0;i<fl.length;i++)
	    	myImgs[i] = new dataSaveImage();
	 
	    Object[][] data=new Object[fl.length][4 + nCut];
	    String[] columnNames = {"Image", "Name", "Volume", "Max", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10"};
	    	        
	    for(i=0;i<fl.length;i++) 
		{
	    	o = new ij.io.Opener();	    	
		 	IJ.showStatus((int)((i/(float)fl.length)*100)+"% ("+fl[i].getName()+")"); // Affichage du chargement
	    	
	    	ImagePlus IPTmp2 = o.openImage(fl[i].getPath());

	    	ij.process.ImageConverter IC = new ij.process.ImageConverter(IPTmp2);
	    	IC.convertToHSB();
	    	
	    	ImagePlus IPTmpS = new ImagePlus(fl[i].getPath(), IPTmp2.getStack().getProcessor(2));
	    	ImagePlus IPTmpH = new ImagePlus(fl[i].getPath(), IPTmp2.getStack().getProcessor(3));
	    	
	    	// Pré-traitements
	    	IPTmpS.getChannelProcessor().invert();
	    	fixBackgroundToZero(IPTmpS.getChannelProcessor());
	    	fixBackgroundToZero(IPTmpH.getChannelProcessor());
	    	
	    	//extraction des données de l'image courante
	    	myImgs[i].icon.icon = new ImageIcon(new ImagePlus("",o.openImage(fl[i].getPath()).getChannelProcessor().resize(150)) .getImage());
	    	myImgs[i].icon.ID = i;
	    	myImgs[i].Volume1 = (float)(((float)getVolume(IPTmpS.getChannelProcessor()))/1 000 000.0);
	    	myImgs[i].max = getMax(IPTmpS.getChannelProcessor());
	    	myImgs[i].posMax = getMaxPos(IPTmpS.getChannelProcessor());
	    	myImgs[i].name = fl[i].getName();
	    	myImgs[i].roiTache  = new Roi[nCut]; 
	    	
	    	int type = Wand.allPoints()?Roi.FREEROI:Roi.TRACED_ROI;
	    	
	    	myImgs[i].surface = new int[nCut];
	    	
	    	Wand w = getWandByThresholdLevel (IPTmpS.duplicate(),150);
			
			myImgs[i].roiTache[0]  = new PolygonRoi(w.xpoints, w.ypoints, w.npoints, type);
			
			int nC;
	    	for(nC=0;nC<nCut;nC++) 
			{
	    		Wand wTmp = getWandByThresholdLevel (IPTmpS.duplicate(),120+(int)(127.0*(float)((float)nC/(float)nCut)));
	    		
	    		if(wTmp.npoints != 4)
	    		{
	    			myImgs[i].roiTache[nC]  = new PolygonRoi(wTmp.xpoints, wTmp.ypoints, wTmp.npoints, type);
	    			myImgs[i].surface[nC] = (int) getArea(myImgs[i].roiTache[nC].getPolygon());
	    			//myImgs[i].surface[nC] = wTmp.npoints;
	    		}
	    		else
	    		{
	    			myImgs[i].roiTache[nC]  = new Roi(0,0,1,1);
	    			myImgs[i].surface[nC] = 0;
	    		}
	    		
	    		
			}
	    	int p;
	    	float xMoy = 0;
	    	float yMoy = 0;
	    	
	    	myImgs[i].barX = (int)(xMoy / w.npoints);
	    	myImgs[i].barY = (int)(yMoy / w.npoints);
	    	
	    	data[i][0] = myImgs[i].icon;
	    	data[i][1] = fl[i].getName();
	    	data[i][2] = myImgs[i].Volume1;
	    	data[i][3] = myImgs[i].max;
	    	for(nC=0;nC<nCut;nC++) 
			{
	    		data[i][4+nC] = myImgs[i].surface[nC];
			}
			
	    	System.gc();
	 	    System.gc();
		}
	    
	    IJ.showStatus("100%");
	    
	    o = null;

	    DefaultTableModel dtmTableModel = new DefaultTableModel (data,columnNames)
	    {
	    	private static final long serialVersionUID = 1L;
	    	public boolean isCellEditable(int iRowIndex, int iColumnIndex)
	    	{
	    		return false;
	    	}
	    	@Override
	    	public Class<?> getColumnClass(int col) 
	    	{
	    		if((col == 2) || (col == 3) || (col == 4)) 
	    		{
	    			return Float.class;
	    		}
	    		if(col == 5 || (col == 6) || (col == 7) || (col == 8) || (col == 9) || (col == 1) || (col == 3) || (col == 3) || (col == 3)) 
	    		{
	    			return Integer.class;
	    		}
	    		return super.getColumnClass(col);
	    	}
	    	
	    };
	    table = new JTable(dtmTableModel);
	    

	    table.getColumnModel().getColumn(0).setCellRenderer(new ImageRenderer());
	    table.setRowHeight(100);
	    table.getTableHeader().setReorderingAllowed(false);

	    table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);
        
	    JScrollPane scrollPane = new JScrollPane(table);
	    
	    button = new JButton("Save .csv");
	    
	    panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		
	    panel.add(scrollPane);
		panel.add(button);
	    instance.add(panel);
	    
	    addAllEvent();

	    TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());   
	    table.setRowSorter(sorter);
	    sorter.setSortable(0, false);
	    
	    instance.pack();
	    instance.setSize(750,950);
	    instance.setLocation( 0,0);
	    instance.show(); 
	}
	
	/* getArea
	 * Retourne l'aire du polygone selectionné
	 * */
	final double getArea(Polygon p) {
        int carea = 0;
        int iminus1;
        for (int i=0; i<p.npoints; i++) {
            iminus1 = i-1;
            if (iminus1<0) iminus1=p.npoints-1;
            carea += (p.xpoints[i]+p.xpoints[iminus1])*(p.ypoints[i]-p.ypoints[iminus1]);
        }
        return (Math.abs(carea/2.0));
    }
	
	private void addAllEvent()
	{
		
		table.setTableHeader(new JTableHeader(table.getTableHeader().getColumnModel())
		{ 
			private static final long serialVersionUID = 1L;

			public void processMouseMotionEvent(MouseEvent me)
			{
				int column = table.getTableHeader().columnAtPoint(me.getPoint());
				if(column == 0)
				{
					setCursor(new Cursor(Cursor.HAND_CURSOR));
				}
				else
				{
					super.processMouseMotionEvent(me);
				}
			}
		}
				);

		
		
		
		
		 /* ***************************************************** */
		 /* GESTION DE L INTERACTION DE LA SOURIS AVEC LE TABLEAU */
		 /* Afin de Minimiser/Agrandire les previews des images   */
		 /* ***************************************************** */
		 table.getTableHeader().addMouseListener(
		    		new MouseAdapter()
		    		{
		    			public void mouseClicked(MouseEvent e)
		    			{
		    				int tableC = table.columnAtPoint(e.getPoint());
		    				if (tableC == 0)
		    				{
		    					if (miniMod)
		    					{
		    						table.setRowHeight(100);    
		    						miniMod = false;
		    					}
		    					else
		    					{
		    						table.setRowHeight(20);    
		    						miniMod = true;
		    					}
		    				}
		    			}
		    		}

		    		);
		 
		 
		 button.addMouseListener(

				 new MouseAdapter()
		    		{
					 	public void mouseClicked(MouseEvent e)
		    			{ 
					 		toCsv();
		    			}
		    		}

		    			
				 );
		 /* ***************************************************** */
		 /* GESTION DE L INTERACTION DE LA SOURIS AVEC LE PLUG IN */
		 /* ***************************************************** */
		 
		 table.addMouseListener(
		    		new MouseAdapter()
		    		{
		    			public void mouseClicked(MouseEvent e)
		    			{
		    				int tableC = table.columnAtPoint(e.getPoint());
		    				int tableR = table.rowAtPoint(e.getPoint());
		    				if (tableC == 0)
		    				{
		    					double scaleLoc = 0.3;
		    					
		    					int ID = ((iconStock)table.getValueAt(tableR, tableC)).ID;
		    					
		    					ImagePlus tmpIp = IJ.openImage(fl[ID].getPath());
		    			    	ImagePlus tmpIp2 = tmpIp;
		    			    	
		    			    	tmpIp2.killRoi();
		    			    	tmpIp2.show();
		    			    	tmpIp2.setDimensions(100, 100, 100);
		    			    	tmpIp2.getCanvas().setMagnification(scaleLoc);
		    					
		    					
		    					Overlay overlay = new Overlay();
		    			
		    					
		    					int ovalSize = 50;
		    					
		    					OvalRoi oval = new OvalRoi(myImgs[ID].posMax.x - (ovalSize/2), myImgs[ID].posMax.y - (ovalSize/2), ovalSize, ovalSize);
		    					oval.setStrokeWidth(2);
		    					oval.setStrokeColor(Color.red);
		    					overlay.add(oval);
		    					
		    					int nC;
		    			    	for(nC=nCut-1;nC>=0;nC--) 
		    					{
		    			    		Color c1 = new Color((float)((float)nC/(float)nCut), (float)1.0-(float)((float)nC/(float)nCut), (float)0.0, (float)1.0);
		    			    		Color c2 = new Color((float)((float)nC/(float)nCut), (float)1.0-(float)((float)nC/(float)nCut), (float)0.0, (float)0.1);
		    			    		
		    			    		Roi roiTmp1 = (Roi)myImgs[ID].roiTache[nC].clone();
		    			    		Roi roiTmp2 = (Roi)myImgs[ID].roiTache[nC].clone();
		    			    		
		    			    		roiTmp1.setStrokeColor(c1);
		    			    		roiTmp1.setStrokeWidth((float) 1);
			    					overlay.addElement(roiTmp1);
			    					roiTmp2.setFillColor(c2);
			    					overlay.addElement(roiTmp2);
		    					}
	    					
		    		            tmpIp2.setOverlay(overlay);				
		    		            tmpIp2.getCanvas().setDrawingSize((int)(tmpIp.getWidth()*scaleLoc),(int)(tmpIp.getHeight()*scaleLoc));		    					
		    		            tmpIp2.getWindow().pack();
		    		            tmpIp2.getCanvas().repaint();
		    		            
		    		            ImagePlus tmpIp3 = IJ.openImage(fl[ID].getPath());
		    		            
		    		            tmpIp3.getProcessor().setColor(Color.white);
		    		            tmpIp3.getProcessor().fill();
		    		            tmpIp3.killRoi();
		    		            
		    		            Overlay overlay2 = new Overlay();
		    		            
		    		            int im;
		    		            for(im = 0;im<myImgs.length;im++)
		    		            {
		    		            	OvalRoi ovalTmp = new OvalRoi(myImgs[im].barX - (ovalSize/2), myImgs[im].barY - (ovalSize/2), ovalSize, ovalSize);
		    		            	ovalTmp.setStrokeWidth(2);
		    		            	ovalTmp.setStrokeColor(Color.blue);
		    		            	overlay2.add(ovalTmp);
		    		            }
		    		            tmpIp3.setOverlay(overlay2);   		            
		    				}
		    			}
		    		}

		    		);
		 
		 
		
	}

	/* Algorithme du peigne 
	 * Permet de nettoyer l'image des bruits parasites 
	 * Ou des resultats indésirables de certain traitements*/
	private void testTache(ImageProcessor i)
	{

		int size = 40;
		
		int u, v, n;
		for (v = 0; v < i.getHeight()-(size+1); v++) 
		{
			for (u = 0; u < i.getWidth(); u++)
			{
				if (((i.getPixel(u,v) & 0xff) == (i.getPixel(u,v+size) & 0xff)) && ((i.getPixel(u,v+size) & 0xff) == 255) )
				{
					for (n = 0; n < size; n++)
					{
						i.set(u, v+n, 0xffffff);
					}
				}
			}
		}		
		
		
		
		
		for (v = 0; v < i.getHeight(); v++) 
		{
			for (u = 0; u < i.getWidth()-(size+1); u++)
			{
				
				if (((i.getPixel(u,v) & 0xff) == (i.getPixel(u+size,v) & 0xff)) && ((i.getPixel(u+size,v) & 0xff) == 255) )
				{
					for (n = 0; n < size; n++)
					{
						i.set(u+n, v, 0xffffff);
					}
				}
				
			
			}
		}		

	}
	
	
	/* fixBackgroundToZero
	 * Permet de compenser la variation de luminosité des images
	 * en ramenant le fond de l'image a zéro
	 * et Translation simple des points 
	 * */
	private void fixBackgroundToZero(ImageProcessor ip)
	{
		int n = 0;
		for (n = 0; n < ip.getPixelCount(); n++) 
		{
			if((  (ip.get(n) & 0xff) == 0) || ((ip.get(n) & 0xff) == 1) || ((ip.get(n) & 0xff) == 2) || ((ip.get(n) & 0xff) == 3) || ((ip.get(n) & 0xff) == 4) || ((ip.get(n) & 0xff) == 5) )
				ip.set(n, 0xffffff);
		}
		
		int [] data = new int[ip.getWidth()];
		java.util.Arrays.fill(data, 0);
		
		ip.getRow(0, 0, data, ip.getWidth());
		java.util.Arrays.sort(data);
		ip.add(255 - (double)(data[data.length/2] & 0xFF));
	}
	
	/* getVolume
	 * Retourne le volume de l'image
	 * Ici on considere l'image comme une nappe avec une élévation
	 * Le volume et le volume sous la nappe
	 * */
	private long getVolume(ImageProcessor i)
	{
		int n = 0;
		long vol = 0;
		for (n = 0; n < i.getPixelCount(); n++) 
		{
			vol = vol + (0xff - (i.get(n) & 0xff)) ;
		}
		return vol;
	}
	
	/* getMax
	 * Permet de connaitre la valeur du pic d'intensité maximale dans l'aile
	 * */
	private long getMax(ImageProcessor i)
	{
		Point pRet = new Point();
		int maxTmp = 255;
	
		int u, v;
		for (v = 0; v < i.getHeight(); v++) 
		{
			for (u = 400; u < i.getWidth(); u++)
			{
				if( maxTmp > (i.getPixel(u,v) & 0xff) )
				{
					maxTmp =  ( (i.getPixel(u,v) & 0xff) );
					pRet.x = u;
					pRet.y = v;
				}	
			}
		}		
		
		
		return 255 - maxTmp;
	}
	
	/* getMaxPos
	 * Retourne les coordonnées en x et y du point d'intensité maximale dans l'aile
	 * */
	private Point getMaxPos(ImageProcessor i)
	{
		Point pRet = new Point();
		int maxTmp = 255*3;
	
		int u, v;
		for (v = 0; v < i.getHeight(); v++) 
		{
			for (u = 400; u < i.getWidth(); u++)
			{
				if( maxTmp > (i.getPixel(u,v) & 0xff) )
				{
					maxTmp =  ( (i.getPixel(u,v) & 0xff) );
					pRet.x = u;
					pRet.y = v;
				}	
			}
		}		
		
		
		return pRet;
	}
	
	
	/* getWandByThresholdLevel
	 * La fonction retourne la ROI correspondant à la coupe de la nappe 
	 * Coupe faite a la hauteur définis pas le niveau lvl (en pourcentage)
	 * */
	private Wand getWandByThresholdLevel(ImagePlus IP, int lvl)
	{
		IP.getChannelProcessor().threshold(lvl);
		
		ImagePlus IPbk = IP.duplicate();
		
    	testTache(IP.getChannelProcessor());
		for (int i=0; i<12; i=i+1 ) IP.getChannelProcessor().dilate();
    	
    	IP = and(IP.duplicate(),IPbk.duplicate());

		for (int i=0; i<15; i=i+1 ) IP.getChannelProcessor().smooth();
    	
    	IP.getChannelProcessor().threshold(127);
    	
		Wand w = new Wand(IP.getChannelProcessor());
		//Position approx de la tache
		w.autoOutline(1200, 150);
		
		return w;
	}
	
	// Palier au probleme pourri du du du de l'aklgo du peigne
	private ImagePlus and(ImagePlus IP1 ,ImagePlus IP2)
	{
		ImageProcessor i1 = IP1.getChannelProcessor();
		ImageProcessor i2 = IP2.getChannelProcessor();
		
		ImagePlus IPR = IP1.duplicate();
		

		int n = 0;
		for (n = 0; n < i1.getPixelCount(); n++) 
		{
			if ((i1.get(n) & 0xff) == 0 && (i2.get(n) & 0xff) == 0)
			{
				IPR.getChannelProcessor().set(n, 0);
			}
			else
			{
				IPR.getChannelProcessor().set(n, 255);
			}
					
		}
		return IPR;
	}
	
	private void toCsv()
	{
	try 
		{
			javax.swing.JFileChooser jfc = new javax.swing.JFileChooser(ij.io.OpenDialog.getDefaultDirectory());
			jfc.setSelectedFile(new File("save.csv"));
			jfc.showSaveDialog(null);
			ij.io.OpenDialog.setDefaultDirectory(jfc.getCurrentDirectory().getPath());
			
			FileWriter writer = new FileWriter(jfc.getSelectedFile());
			
			int i;
			
			writer.append("Name");
			writer.append(';');
			writer.append("Volume");
			writer.append(';');
			writer.append("Max");
			writer.append(';');
			writer.append("S1");
			writer.append(';');
			writer.append("S2");
			writer.append(';');
			writer.append("S3");
			writer.append(';');
			writer.append("S4");
			writer.append(';');
			writer.append("S5");
			writer.append(';');
			writer.append("S6");
			writer.append(';');
			writer.append("S7");
			writer.append(';');
			writer.append("S8");
			writer.append(';');
			writer.append("S9");
			writer.append(';');
			writer.append("S10");
			writer.append('\n');
			
			for(i=0;i<fl.length;i++) 
			{
				writer.append(myImgs[i].name);
				writer.append(';');
				writer.append(Float.toString(myImgs[i].Volume1));
				writer.append(';');
				writer.append(Long.toString(myImgs[i].max));
				int nC;
				for(nC=0;nC<nCut;nC++) 
				{
					writer.append(';');
					writer.append(Integer.toString(myImgs[i].surface[nC]));
				}
				writer.append('\n');
			}
			
			writer.flush();
			writer.close();
		} 
		
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}
	

}
