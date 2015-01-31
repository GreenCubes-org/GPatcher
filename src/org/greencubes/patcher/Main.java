package org.greencubes.patcher;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.greencubes.util.I18n;
import org.greencubes.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Main {
	
	private static boolean silent = true;
	private static JFrame frame = null;
	private static JLabel statusPane = null;
	
	public static void main(String[] args) {
		System.setProperty("awt.useSystemAAFontSettings","on");
		System.setProperty("swing.aatext", "true");
		System.setProperty("java.net.preferIPv4Stack", "true");
		if(args.length < 1)
			throw new IllegalArgumentException("You must specify path to patch file");
		JSONObject patchFile = null;
		File patchF = new File(args[0]);
		FileReader fr = null;
		try {
			fr = new FileReader(patchF);
			patchFile = new JSONObject(new JSONTokener(fr));
		} catch(Exception e) {
			error("Unable to read patch file '" + args[0] + "'!", "", e);
			return;
		} finally {
			Util.close(fr);
		}
		
		silent = patchFile.optBoolean("silent", silent);
		if(!silent) {
			ArrayList<BufferedImage> icons = new ArrayList<BufferedImage>();
			try {
				icons.add(ImageIO.read(Main.class.getResource("/res/icons/gcico32x32.png")));
				icons.add(ImageIO.read(Main.class.getResource("/res/icons/gcico48x48.png")));
				icons.add(ImageIO.read(Main.class.getResource("/res/icons/gcico64x64.png")));
				icons.add(ImageIO.read(Main.class.getResource("/res/icons/gcico128x128.png")));
				icons.add(ImageIO.read(Main.class.getResource("/res/icons/gcico256x256.png")));
			} catch(IOException e) {}
			frame = new JFrame("Patcher");
			frame.setIconImages(icons);
			frame.setUndecorated(true);
			final JPanel container;
			frame.add(container = new JPanel() {{
				Dimension d = new Dimension(400, 100);
				setPreferredSize(d);
				setBackground(new Color(11, 24, 24, 255));
				setBorder(BorderFactory.createLineBorder(new Color(35, 61, 58, 255), 1));
				setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			}});
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						container.add(new JLabel() {{
							setText(I18n.get("title"));
							setOpaque(false);
							setBackground(new Color(0, 0, 0, 0));
							setForeground(new Color(236, 255, 255, 255));
							setFont(new Font("Dialog", Font.BOLD, 20));
							disableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
							setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
						}});
						container.add(statusPane = new JLabel() {{
							setOpaque(false);
							setBackground(new Color(0, 0, 0, 0));
							setForeground(new Color(236, 255, 255, 255));
							setFont(new Font("Dialog", Font.PLAIN, 14));
							disableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
							setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
						}});
						frame.revalidate();
					}
				});
			} catch(Exception e) {
				error(e.getLocalizedMessage(), "", e);
			}
		}
		
		JSONArray patchFiles = patchFile.optJSONArray("files");
		if(patchFiles == null) {
			error(I18n.get("patch-error"), I18n.get("error-no", 1), null);
			return;
		}
		System.out.println("[SIG] LOADED");
		long waitTime = patchFile.optLong("delay", 1000);
		if(waitTime > 0) {
			setStatus(I18n.get("waiting"));
			try {
				Util.sleepForReal(waitTime);
			} catch(InterruptedException e) {}
		}
		
		setStatus(I18n.get("checking"));
		for(int i = 0; i < patchFiles.length(); ++i) {
			JSONObject fo = patchFiles.optJSONObject(0);
			if(fo == null) {
				error(I18n.get("patch-error"), I18n.get("error-no", 2), null);
				return;
			}
			File source = new File(fo.optString("src"));
			if(!source.exists()) {
				error(I18n.get("patch-error"), I18n.get("error-no", 3), null);
				return;
			}
			File target = new File(fo.optString("target"));
			try {
				Util.canReplaceFile(target);
			} catch(IOException e) {
				try {
					Util.sleepForReal(1000); // Wait one more second and try again
				} catch(InterruptedException e1) {}
				try {
					Util.canReplaceFile(target);
				} catch(IOException e2) {
					error(I18n.get("patch-error"), I18n.get("error-no", 4), e2);
					return;
				}
			}
		}
		
		setStatus(I18n.get("updating-files"));
		for(int i = 0; i < patchFiles.length(); ++i) {
			JSONObject fo = patchFiles.optJSONObject(0);
			File source = new File(fo.optString("src"));
			File target = new File(fo.optString("target"));
			if(target.exists() && !target.delete()) {
				try {
					Util.sleepForReal(1000); // Wait one more second and try again
				} catch(InterruptedException e1) {}
				if(target.exists() && !target.delete()) {
					error(I18n.get("patch-error"), I18n.get("error-no", 5), null);
					return;
				}
			}
			source.renameTo(target);
		}
		
		setStatus(I18n.get("starting"));
		if(!startProcess(patchFile))
			return;
		
		String patchDir = patchFile.optString("patchdir");
		if(patchDir != null) {
			File pd = new File(patchDir);
			if(pd.exists())
				Util.deleteDirectory(pd);
		}
		patchF.delete();
		System.out.println("[SIG] END");
		System.exit(0);
	}
	
	private static boolean startProcess(JSONObject patchFile) {
		JSONArray execArray = patchFile.optJSONArray("exec");
		if(execArray == null || execArray.length() == 0)
			System.exit(0);
		List<String> command = new ArrayList<String>();
		for(int i = 0; i < execArray.length(); ++i) {
			command.add(execArray.optString(i));
		}
		Process process = null;
		ProcessBuilder pb = new ProcessBuilder(command);
		try {
			process = pb.start();
		} catch(Exception e) {
			try {
				Util.sleepForReal(1000); // Wait one more second and try again
			} catch(InterruptedException e1) {}
			try {
				process = pb.start();
			} catch(Exception e2) {
				error(I18n.get("patch-error"), I18n.get("error-no", 6), e2);
				return false;
			}
		}
		// Wait few seconds and check that everyting is OK
		setStatus(I18n.get("waiting"));
		try {
			Util.sleepForReal(5000);
		} catch(InterruptedException e1) {}
		try {
			int exitValue = process.exitValue();
			if(exitValue != 0) {
				error(I18n.get("patch-error"), patchFile.optString("onerror", I18n.get("launch-error")), null);
				return false;
			}
		} catch(IllegalThreadStateException ex) {
			// Process is still running - its fine
		}
		return true;
	}
	
	public static void setStatus(final String status) {
		if(statusPane != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					statusPane.setText(status);
					frame.revalidate();
					frame.repaint();
				}
			});
		}
	}
	
	public static void error(String title, String message, Throwable exception) {
		System.out.println("[SIG] ERROR");
		// TODO : Add UI
		throw (RuntimeException) new RuntimeException(title + "\n" + message).initCause(exception);
	}
}