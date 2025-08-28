package mlbb_Images;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.security.InvalidKeyException;

public class ModifiableImageApp extends JFrame {
	// List of characters
	private final String[] characters = {"lesley", "lunox",
			"cici", "esmeralda", "ixia", "melissa",
			"fanny", "guinevere", "benedetta", "odette",
			"masha", "mathilda", "kagura", "layla"};
	
	private int width, height;
	private BufferedImage canvas;
	private File imgFile;
	private ImageLoader imgLdr;
	
	private static final boolean REDRAW_PRINT = true;
	private static final boolean CIPHER_PRINT = false;
	
	public ModifiableImageApp() {
		super("AES Image Encryption");
		
		File defaultFile = new File("data/after-sunset-minimal-4k-zm-1920x1080.jpg");
		
		// 1) Create your drawing surface
		loadFile(defaultFile);
		redraw(width, height, REDRAW_PRINT);
		
		// 2) CENTER: a viewer that paints the canvas, scales to fit, and enforces canvas min size
		JComponent canvasViewer = new JPanel() {
			@Override public Dimension getPreferredSize() {
				return new Dimension(canvas.getWidth(), canvas.getHeight());
			}
			@Override public Dimension getMinimumSize() {
				return new Dimension(320, Math.min(height, 180));
			}
			@Override protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (canvas == null) return;
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				
				int srcW = canvas.getWidth();
				int srcH = canvas.getHeight();
				int dstW = getWidth();
				int dstH = getHeight();
				double s = Math.min(dstW / (double)(srcW), dstH / (double)(srcH));
				int w = (int)(Math.round(srcW * s));
				int h = (int)(Math.round(srcH * s));
				int x = (dstW - w) / 2;
				int y = (dstH - h) / 2;
				g2.drawImage(canvas, x, y, w, h, null);
				g2.dispose();
			}
		};
		
		// 3) SOUTH: 52px-high menu with vertical scrolling
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS)); // vertical stack; scroll vertically
		controls.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		
		JScrollPane controlScroll = new JScrollPane(
				controls,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
		);
		controlScroll.setBorder(null);
		controlScroll.setViewportBorder(null);
		controlScroll.getVerticalScrollBar().setUnitIncrement(12);
			
		JPanel southBar = new JPanel(new BorderLayout());
		southBar.add(controlScroll, BorderLayout.CENTER);
		southBar.setPreferredSize(new Dimension(0, 52)); // fixed 52px tall
		
		JButton encryptButtonARGB = new JButton(new AbstractAction("Encrypt using ARGB") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					imgLdr.encryptImageARGB(CIPHER_PRINT);
					redraw(width, height, REDRAW_PRINT);
					canvasViewer.repaint();
				} catch (InvalidKeyException e1) {
					System.out.println("No key avaliable.");
				} catch (Exception e1) {
					System.out.println("Encryption failed, please try again later.");
				}
			}
		});
		
		controls.add(encryptButtonARGB);
		
		JButton decryptButtonARGB = new JButton(new AbstractAction("Decrypt using ARGB") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					imgLdr.decryptImageARGB(CIPHER_PRINT);
					redraw(width, height, REDRAW_PRINT);
					canvasViewer.repaint();
				} catch (InvalidKeyException e1) {
					System.out.println("No key avaliable. Please encrypt the image first before decrypting.");
				} catch (Exception e1) {
					System.out.println("Decryption failed, please try again later.");
				}
			}
		});
		
		controls.add(decryptButtonARGB);
		
		JButton encryptButtonRGB = new JButton(new AbstractAction("Encrypt using RGB") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					imgLdr.encryptImageRGB(CIPHER_PRINT);
					redraw(width, height, REDRAW_PRINT);
					canvasViewer.repaint();
				} catch (InvalidKeyException e1) {
					System.out.println("No key avaliable.");
				} catch (Exception e1) {
					System.out.println("Encryption failed, please try again later.");
				}
			}
		});
		
		controls.add(encryptButtonRGB);
		
		JButton decryptButtonRGB = new JButton(new AbstractAction("Decrypt using RGB") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					imgLdr.decryptImageRGB(CIPHER_PRINT);
					redraw(width, height, REDRAW_PRINT);
					canvasViewer.repaint();
				} catch (InvalidKeyException e1) {
					System.out.println("No key avaliable. Please encrypt the image first before decrypting.");
				} catch (Exception e1) {
					System.out.println("Decryption failed, please try again later.");
				}
			}
		});
		
		controls.add(decryptButtonRGB);
		
		JButton addOne = new JButton(new AbstractAction("Add 1") {
			@Override
			public void actionPerformed(ActionEvent e) {
				imgLdr.addOne();
				redraw(width, height, REDRAW_PRINT);
				canvasViewer.repaint();
			}
		});
		
		controls.add(addOne);
		
		JButton addTen = new JButton(new AbstractAction("Add 10") {
			@Override
			public void actionPerformed(ActionEvent e) {
				imgLdr.addTen();
				redraw(width, height, REDRAW_PRINT);
				canvasViewer.repaint();
			}
		});
		
		controls.add(addTen);
		
		JButton subtractOne = new JButton(new AbstractAction("Subtract 1") {
			@Override
			public void actionPerformed(ActionEvent e) {
				imgLdr.subtractOne();
				redraw(width, height, REDRAW_PRINT);
				canvasViewer.repaint();
			}
		});
		
		controls.add(subtractOne);
		
		JButton subtractTen = new JButton(new AbstractAction("Subtract 10") {
			@Override
			public void actionPerformed(ActionEvent e) {
				imgLdr.subtractTen();
				redraw(width, height, REDRAW_PRINT);
				canvasViewer.repaint();
			}
		});
		
		controls.add(subtractTen);
		
		JButton loadButton = new JButton(new AbstractAction("Load") {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				if (chooser.showOpenDialog(ModifiableImageApp.this) != JFileChooser.APPROVE_OPTION) return;
				File f = chooser.getSelectedFile();
				loadFile(f);
				redraw(width, height, REDRAW_PRINT);
				packFrame(canvasViewer, southBar);
				canvasViewer.repaint();
			}
		});
		
		controls.add(loadButton);
		
		JButton saveButton = new JButton(new AbstractAction("Save as PNG") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					imgLdr.saveAsPNG(getHeroName(imgFile.getName()));
				} catch (NullPointerException e1) {
					System.out.println("No key avaliable. Please encrypt the image first before saving.");
				}
			}
		});
		
		controls.add(saveButton);
		
		// 4) Layout the frame
		setLayout(new BorderLayout());
		add(canvasViewer, BorderLayout.CENTER);
		add(southBar, BorderLayout.SOUTH);
		
		// 5) Pack, then set a correct *frame* minimum size that includes insets + 52px menu
		pack();
		packFrame(canvasViewer, southBar);
		
		// Optional initial size
		setSize(1280, 800);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	private void redraw(int width, int height, boolean printInfo) {
		int[] rgbData = imgLdr.getRgbArray(printInfo);
		canvas.setRGB(0, 0, width, height, rgbData, 0, width);
	}
	
	private void packFrame(JComponent canvasViewer, JPanel southBar) {
		// Let layout recompute sizes based on current canvas
		getContentPane().revalidate();
		Insets ins = getInsets();
		Dimension viewerMin = canvasViewer.getMinimumSize();
		int southH = southBar.getPreferredSize().height;
		int frameMinW = viewerMin.width + ins.left + ins.right;
		int frameMinH = viewerMin.height + southH + ins.top + ins.bottom;
		setMinimumSize(new Dimension(frameMinW, frameMinH));
	}
	
	private void loadFile(File f) {
		try {
			imgFile = f;
			
			// Construct loader based on whether you pass a file:
			if (f == null) {
				imgLdr = new ImageLoader();
			} else {
				imgLdr = new ImageLoader(f);
			}
			
			width = imgLdr.getImgWidth();
			height = imgLdr.getImgHeight();
			
			// Recreate canvas at the new size
			canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Failed to load " + f, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private String getHeroName(String fileName) {
		for (int i = 0; i < characters.length; i++) {
			if (fileName.contains(characters[i])) {
				return characters[i];
			}
		}
		return "encrypted";
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(ModifiableImageApp::new);
	}
}
