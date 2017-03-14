import java.util.*;
import java.lang.*;
import java.lang.Object.*;
import java.io.*;
import java.nio.file.*;
import java.math.*;
import java.awt.*;
import java.awt.List;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.JList;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioSystem;

class A
{
    public static void main(String args[]) throws Exception
    {
        new Win();
        return;
    }
}

class Win extends JFrame
{
    public Audio audio;
    public UserListener userListener;
    public List songList;
    public JButton delButton, addButton, playButton, chngName;
    public JPanel buttons, head, changeName, changeSong;
    public TextField songName;
    public LoadLine loadLine;

    public Win() throws Exception
    {
        super();
        setSize(250,350);
        setLocation(100,100);
        setLayout(new BorderLayout());
//      getContentPane().setBackground((new Color(34, 34, 34)));
//      addWindowListener(new MyWindowAdapter());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 

        audio = new Audio(this);
        userListener = new UserListener(this);

        songList = new List();
        for (int i = 0; i < audio.clipsCnt; i++)
        {
            songList.add(audio.listNames[i], 0);
        } 
        songList.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent e)
                {
                    audio.playFromList(audio.clipsCnt - songList.getSelectedIndex() - 1);
                    songName.setText(songList.getSelectedItem());
                }
            });
//      songList.select(0);
        add(songList);
        
        buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 2, 0, 0));
        
        delButton = new JButton("Delete");
        delButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (songList.getSelectedIndex() == -1)
                        return;
                    audio.delSong(audio.clipsCnt - songList.getSelectedIndex() - 1);
                    return;
                }
            });
        
        addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    JFileChooser fileopen = new JFileChooser();
                    int ret = fileopen.showDialog(null, "Открыть файл");                
                    if (ret == JFileChooser.APPROVE_OPTION)
                    {
                        File newSong = fileopen.getSelectedFile();
                        audio.addSong(newSong.getAbsolutePath() + " new Song");
                    }
                    return;
                }
            });
        
        buttons.add(delButton);
        buttons.add(addButton);
        
        add(buttons, BorderLayout.PAGE_END);
        
        head = new JPanel();
        head.setLayout(new BorderLayout());
        playButton = new JButton("play    ");
        playButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (songList.getSelectedIndex() == -1)
                        return;
                    if (audio.clip.isRunning())
                        audio.pause();
                    else
                        audio.cont();
                    return;
                }
            });
        
        changeSong = new JPanel();
        changeSong.setLayout(new BorderLayout());
        changeName = new JPanel();
        changeName.setLayout(new BorderLayout());

        loadLine = new LoadLine(this);
        changeSong.add(playButton, BorderLayout.EAST);
        changeSong.add(loadLine, BorderLayout.CENTER);


        chngName = new JButton("change");
        chngName.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (songList.getSelectedIndex() == -1)
                        return;
                    audio.updSong(songList.getSelectedItem(), songName.getText());
                    return;
                }
            });

        songName = new TextField("");
        
        changeName.add(songName, BorderLayout.CENTER);
        changeName.add(chngName, BorderLayout.EAST);
        
        head.add(changeName, BorderLayout.PAGE_START);
        head.add(changeSong, BorderLayout.PAGE_END);
        
        
        add(head, BorderLayout.PAGE_START);
        setVisible(true);
    }
}

class LoadLine extends JPanel implements MouseListener, Runnable
{
    Win win;
    int thisHover;
    public double savedShare;

    public LoadLine(Win win)
    {
        super();
        this.win = win;
        thisHover = 0;
        new Thread(this).start();
        addMouseListener(this);
    }

    public void paint(Graphics g)
    {
       super.paint(g);
//     if (!win.audio.clip.isRunning())
//          return;
        double share;
        if (win.audio.playBool == 1)
            share = (double)win.audio.clip.getMicrosecondPosition() / (double)win.audio.clip.getMicrosecondLength();
        else
            share = (double)win.audio.clipPosition / (double)win.audio.clipLength;
        System.out.println(win.audio.playBool);
        System.out.println(share);
        Dimension sz = getSize();
        g.setColor(Color.black);
        int[] xpoints = {0, (int)(share * (sz.width - 1)), (int)(share * (sz.width - 1)), 0};
        int[] ypoints = {0, 0, sz.height - 1, sz.height - 1};
        g.fillPolygon(xpoints, ypoints, 4);
    }

    public void run()
    {
        while (true)
        {
            if (win.songList.getSelectedIndex() == -1)
            {
                try
                {
                    Thread.sleep(100);
                }
                catch(Exception e){System.out.println(e);}
                continue;
            }
            repaint();
            try
            {
                Thread.sleep(100);
            }
            catch(Exception e){System.out.println(e);}
        }
    }


    public void mouseEntered(MouseEvent me)
    {
        System.out.println("YA");
        thisHover = 1;
    }

    public void mouseClicked(MouseEvent e)
    {
        System.out.println("HEy");
        if (thisHover != 1 || win.songList.getSelectedIndex() == -1 || !win.audio.clip.isRunning())
            return;

        Point p = e.getPoint();
        win.audio.rewind((double)p.x / getSize().width);
    }

    public void mouseExited(MouseEvent e)
    {
        thisHover = 0;
    }
    public void mousePressed(MouseEvent e){}
    public void mouseReleased(MouseEvent e){}
}

class Audio
{
    public Win win;
    File soundFile;
    AudioInputStream inputStream;
    public Clip clip;
    public long clipLength, clipPosition;
    public int clipsCnt;
    public String[] listNames = new String[100005];
    public String[] listPaths = new String[100005];
    public int playBool;

    public Audio(Win win)
    {
        this.win = win;
        playBool = 0;
        try
        {
            clip = AudioSystem.getClip();
            LineListener tmp = new TmpLineListener(win, this);
            clip.addLineListener(tmp);
            
        }
        catch(Exception e){System.out.println(e);}
        readList();
    }

    public void playFromList(int id)
    {
        playFromList(listNames[id]);
        return;
    }

    public void playFromList(String name)
    {
        String path;
        int id = idByName(name);
        if (id == -1)
            return;
        play("data/" + listPaths[id]);
        return;
    }
    
    public int idByName(String name)
    {
        for (int i = 0; i < clipsCnt; i++)
        {
            if (name.equals(listNames[i]))
                return i;
        }
        return -1;      
    }
    

    public void play(String path)
    {
        try
        {
            soundFile = new File(path);
            inputStream = AudioSystem.getAudioInputStream(soundFile);
            System.out.println("OpenAudioSystem");
            if (clip.isRunning())
                clip.stop();
            if (clip.isOpen())
                clip.close();
            clip.open(inputStream);
            clipLength = clip.getMicrosecondLength();
            System.out.println("OpenClip");
            clip.start();
            System.out.println("StartClip");
            win.playButton.setText("pause");
            playBool = 1;
        }
        catch(Exception e){System.out.println(e);}
    }

    public void pause()
    {
        try
        {
            System.out.println("pauseStart");
            if (clip.isRunning())
            {
                System.out.println("pause_RUN");
                clipPosition = clip.getMicrosecondPosition();
                playBool = 0;
                clip.stop();
                System.out.println("pause_stop");
                win.playButton.setText("play    ");
            }
        }
        catch(Exception e){System.out.println(e);}
    }


    public void cont()
    {
        try
        {
            System.out.println("continueStart");
            if (clip.isOpen())
            {
                System.out.println("continue");
                clip.start();
                win.playButton.setText("pause");
                playBool = 1;
            }
        }
        catch(Exception e){System.out.println(e);}
    }

    public void rewind(double share)
    {
        try
        {
            if (clip.isOpen())
            {
                pause();
                System.out.println(clipLength);
                System.out.println(share);
                clipPosition = (long)(share * clipLength);
                clip.setMicrosecondPosition(clipPosition);
                cont();
            }
        }
        catch(Exception e){System.out.println(e);}

    }

    public void printList()
    {
        System.out.println("List:");
        for (int i = clipsCnt - 1; i >= 0; i--)
        {
            System.out.println(" - " + listNames[i]);
        }
    }


    public void addSong(String path)
    {
        if (clipsCnt >= 100000)
            return;
        String song[] = path.split(" ");
        String name;
        path = song[0];
        if (song.length == 2)
            name = song[1];
        else
            name = "" + clipsCnt;
/*      for (int i = 0; i < clipsCnt; i++)
            if (listNames[i] == name)
            {
                System.out.println("Song with this name is already exists");
                return;
            }
*/      try
        {
            long kol = clipsCnt;
            File f1 = new File(path);
            File f2 = new File("data/" + kol + ".aiff");
            Files.copy(f1.toPath(), f2.toPath());
            String data = name + " " + kol + ".aiff";
            try
            {
                OutputStream os;
                os = new FileOutputStream(new File("data/.info"), true);
                os.write(data.getBytes(), 0, data.length());
                os.close();
            }
            catch(Exception e){System.out.println(e);}
            listNames[clipsCnt] = name;
            listPaths[clipsCnt] = kol + ".aiff";
            win.songList.add(listNames[clipsCnt], 0);
            clipsCnt++;
        }
        catch(Exception e){System.out.println(e);}
    }
    
    public void updSong(String name, String newName)
    {
/*      for (int i = 0; i < clipsCnt; i++)
            if (listNames[i] == name)
            {
                System.out.println("Song with this name is already exists");
                return;
            }
*/      int id = idByName(name);
        int selectedId = win.songList.getSelectedIndex(); 
        win.songList.replaceItem(newName, clipsCnt - id - 1);
        win.songList.select(selectedId);
        listNames[id] = newName;
        updInfo();
        return;
    }

    public void delSong(String name)
    {
        delSong(idByName(name));
        return;
    }

    public void delSong(int id)
    {
        win.songList.remove(clipsCnt - id - 1);
        clipsCnt--;
        try
        {
            new File("data/" + listPaths[id]).delete();
        }
        catch(Exception e){System.out.println(e);}
        for (int i = id; i < clipsCnt; i++)
        {
            listNames[i] = listNames[i + 1];
            listPaths[i] = listPaths[i + 1];
        }
        updInfo();
        return;
    }

    public void updInfo()
    {
        try
        {
            PrintWriter pw;
            pw = new PrintWriter(new File("data/.info"));
            for (int i = 0; i < clipsCnt; i++)
            {
                pw.println(listNames[i] + " " + listPaths[i]);
            }
            pw.close();
        }
        catch(Exception e){System.out.println(e);}
        return;
    }
    
    
    public void readList()
    {
        try
        {
            Scanner sc = new Scanner(new File("data/.info"));
            clipsCnt = 0;
            while (sc.hasNextLine())
            {
                String st[] = sc.nextLine().split(" ");
                listNames[clipsCnt] = st[0];
                listPaths[clipsCnt] = st[1];
                clipsCnt++;
            }
            sc.close();
        }
        catch(Exception e){System.out.println(e);}
        System.out.println("clipsCnt - " + clipsCnt);
    }

}

class UserListener extends Thread
{
    Win win;
    Audio audio;
    Scanner sc;

    public UserListener(Win win)
    {
        this.win = win;
        audio = win.audio;
        start();
    }

    public void run()
    {
        sc = new Scanner(System.in);
        while (true)
        {
            String st = sc.nextLine();
            if (st.substring(0, 4).equals("open"))
            {
                audio.play(st.substring(5));
                continue;
            }
            if (st.substring(0, 4).equals("play"))
            {
                audio.playFromList(st.substring(5));
                continue;
            }
            if (st.substring(0, 4).equals("cont"))
            {
                audio.cont();
                continue;
            }
            if (st.substring(0, 4).equals("paus"))
            {
                audio.pause();
                continue;
            }
            if (st.substring(0, 4).equals("rwnd"))
            {
                audio.rewind(new Double(st.substring(5)));
                continue;
            }
            if (st.substring(0, 4).equals("list"))
            {
                audio.printList();
                continue;
            }
            if (st.substring(0, 4).equals("add "))
            {
                audio.addSong(st.substring(4));
                continue;
            }
            if (st.substring(0, 4).equals("chng"))
            {
                audio.updSong(st.substring(5).split(" ")[0], st.substring(5).split(" ")[1]);
                continue;
            }
            if (st.substring(0, 4).equals("dele"))
            {
                audio.delSong(st.substring(5));
                continue;
            }
        }
    }
}

class TmpLineListener implements LineListener
{
    Win win;
    Audio audio;
    
    public TmpLineListener(Win win, Audio audio)
    {
        this.win = win;
        this.audio = audio;
    }
    public void update(LineEvent event)
    {
        if (audio.playBool == 1 && !audio.clip.isRunning())
        {
/*          System.out.println("ZASHLI");
            int to;
            if (win.songList.getRows() - 1 == win.songList.getSelectedIndex())
                to = 0;
            else
                to = win.songList.getSelectedIndex() + 1;
            win.songList.select(to);
            audio.playFromList(audio.clipsCnt - to - 1);
*/      }
    }
}