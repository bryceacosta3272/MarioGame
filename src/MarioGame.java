import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.util.Vector;

public class MarioGame {
    public MarioGame() {
        setup();
    }

    public static void setup() {
        appFrame = new JFrame("MarioGame");
        XOFFSET = 0;
        YOFFSET = 40;
        WINWIDTH = 500;
        WINHEIGHT = 500;
        pi = 3.14159265358979;
        twoPi = 2.0 * 3.14159265358979;
        endgame = false;
        p1width = 43; //25;
        p1height = 88; //25;
        p1originalX = 10;
        p1originalY = 320;
        gravity = 0.38; //0.38
        map = new Vector<>();
        ground = new Vector<>();
        cliff = new Vector<>();
        soundPlayer = new SoundPlayer();
        try {
            background = ImageIO.read( new File( "Images/background.png" ) );
            player1 = ImageIO.read( new File( "Images/mario.png" ) );
            goomba = ImageIO.read( new File( "Images/mushroom.png" ) );
        }
        catch( IOException ioe ) {
        }
    }

    private static class Animate implements Runnable {
        public void run() {
            while( endgame == false ) {
                backgroundDraw();
                playerDraw();
                mushroomDraw();
                //testDraw();

                try {
                    Thread.sleep(32);
                }
                catch( InterruptedException e ) {
                }
            }
        }
    }

    private static class SoundPlayer {
        private Clip background;
        private long clipTime = 0;
        private AudioInputStream ais;

        public SoundPlayer() {
            background = getClip(loadAudio("01-main-theme-overworld"));
        }

        private AudioInputStream loadAudio(String url) {
            try {
                ais = AudioSystem.getAudioInputStream(new File("Media/" + url + ".wav"));
                return ais;
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            return null;
        }

        private Clip getClip(AudioInputStream stream) {
            try {
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                return clip;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public void resumeBackground(){
            background.setMicrosecondPosition(clipTime);
            background.start();
        }

        public void pauseBackground(){
            clipTime = background.getMicrosecondPosition();
            background.stop();
        }

        public void restartBackground() {
            clipTime = 0;
            resumeBackground();
        }

        public void playJump() {
            Clip clip = getClip(loadAudio("smb_jump-small"));
            clip.start();

        }

        public void playGameOver() {
            Clip clip = getClip(loadAudio("smb_gameover"));
            clip.start();

        }

        public void playMarioDies() {
            Clip clip = getClip(loadAudio("smb_mariodie"));
            clip.start();

        }
    }

    private static class PlayerMover implements Runnable {
        public PlayerMover() {}

        public void run() {
            while( endgame == false ) {
                try {
                    Thread.sleep( 10 );
                }
                catch( InterruptedException e ) {

                }

                p1.updateJump();

                if (p1upPressed) {
                    p1.jump();
                }

                if (p1rightPressed) {
                    p1velocityX = 3; // 2
                    if (!rightCollision) {
                        if (p1.getX() > 100) {
                            camera.moveCamera(p1velocityX, 0);
                        } else {
                            p1.move(p1velocityX, 0);
                        }
                        if (leftCollision) {
                            leftCollision = false;
                        }
                    }
                }
                if (p1leftPressed) {
                    if (!leftCollision) {
                        p1velocityX = -3;
                        p1.move(-3, 0);
                        if (rightCollision) {
                            rightCollision = false;
                        }
                    }
                }
                updatePlayerBox();
            }
        }
    }


    private static class Camera {
        public Camera() {
            x = 0;
            y = 0;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public void setCamera(double xAmount, double yAmount) {
            x = xAmount;
            y = yAmount;
        }

        public void moveCamera(double xAmount, double yAmount) {
            x = x + xAmount;
            y = y + yAmount;
        }

        public Point getCameraLocation() {
            return new Point ((int) camera.getX(), (int) camera.getY());
        }

        private double x;
        private double y;
    }

    private static class GoombaMover implements Runnable {
        public GoombaMover() {
            velocity = 3.5;
        }

        public void run() {
            while( endgame == false && mushroomAlive == true ) {
                try {
                    Thread.sleep( 10 );
                }
                catch( InterruptedException e ) {
                }

                try {
                    if (camera.getX() > 10) {
                        mushroom.move(-velocity, 0);
                    }
                }
                catch( java.lang.NullPointerException jlnpe ) {
                }
            }
        }
        private double velocity;
    }

    // For Player bullets
    // dist is a distance between the two objects at the top of the inner object.
    private static void lockrotateObjAroundObjtop( ImageObject objOuter, ImageObject objInner, double dist ) {
        objOuter.moveto( objInner.getX() + objOuter.getWidth()
                        + (objInner.getWidth() / 2.0 + (dist + objInner.getWidth() / 2.0 )
                        * Math.cos( objInner.getAngle() + pi/2.0 )) / 2.0,
                objInner.getY() - objOuter.getHeight() + (dist + objInner.getHeight() / 2.0 )
                        * Math.sin( objInner.getAngle() / 2.0 ));
        objOuter.setAngle( objInner.getAngle() );
    }

    private static AffineTransformOp rotateImageObject( ImageObject obj ) {
        AffineTransform at = AffineTransform.getRotateInstance( -obj.getAngle(), obj.getWidth()/2.0, obj.getHeight()/2.0 );
        AffineTransformOp atop = new AffineTransformOp( at, AffineTransformOp.TYPE_BILINEAR );
        return atop;
    }

    private static void backgroundDraw() {
        Graphics g = appFrame.getGraphics();
        Graphics2D g2D = (Graphics2D) g;
        Point cameraLocation = camera.getCameraLocation();
        g.translate(-(int) cameraLocation.getX(), (int) cameraLocation.getY());
        g2D.drawImage( background, XOFFSET, YOFFSET, null );
    }

    private static void playerDraw() {
        Graphics g = appFrame.getGraphics();
        Graphics2D g2D = (Graphics2D) g;
        g2D.drawImage( rotateImageObject( p1 ).filter(player1, null), (int)(p1.getX() + 0.5), (int)(p1.getY() + 0.5), null );
    }

    private static void mushroomDraw() {
        Graphics g = appFrame.getGraphics();
        Graphics2D g2D = (Graphics2D) g;
        g2D.drawImage( rotateImageObject(mushroom).filter(goomba, null), (int)(mushroom.getX() + 0.5), (int)(mushroom.getY() + 0.5), null );
    }

    //for debugging
    private static void testDraw() {
        Graphics g = appFrame.getGraphics();
        Graphics2D g2D = (Graphics2D) g;
        g2D.setColor(Color.black);

        //mushroom
        g2D.fillRect((int) mushroom.getX(), (int) mushroom.getY(), (int) mushroom.getWidth(), (int) mushroom.getWidth());

        //ground
        //g2D.fillRect(0, 421, 2123, 6);

        //p1
        g2D.fillRect((int) (p1.getX()), (int) p1.getY(), (int) p1width, (int) p1height);
        //rightCollisions
        //g2D.fillRect((int) (rightPlayerBox.getX()), (int) p1.getY() + 30, (int)rightPlayerBox.getWidth(), (int) p1.getHeight() - 20);
        //leftCollisions
        //g2D.fillRect((int) (p1.getX() + 23), (int) p1.getY() + 20, 1, (int) p1.getHeight());
        //topCollisions
        //g2D.fillRect((int) (p1.getX() + 32), (int) p1.getY() + 22, (int) p1.getWidth() / 2, 1);
        //bottomCollisions
        g2D.setColor(Color.yellow);
        g2D.fillRect((int) (p1.getX() + 35 + camera.getX()), (int) (p1.getY() + 15 + p1.getHeight()), (int) p1.getWidth() / 2, 1);
    }

    private static class CollisionChecker implements Runnable {
        public void run() {
            while( endgame == false ) {
                try {
                    //prevents player from going too far backwards
                    if (collisionOccurs(leftBounds, p1)) {
                        p1.move(2,0);
                    }

                    p1.falling = checkBottomCollisions();

                    //map collisions
                    for (int i = 0; i < map.size(); i++) {
                        //left collisions
                        if (collisionOccurs(map.elementAt(i), leftPlayerBox)) {
                            leftCollision = true;
                        }
                        //top collisions
                        if (collisionOccurs(map.elementAt(i), topPlayerBox)) {
                            p1.jumping = false;
                            p1.falling = true;
                        }
                        //right collisions
                        if (collisionOccurs(map.elementAt(i), rightPlayerBox)) {
                            rightCollision = true;
                        }

                        if (collisionOccurs(mushroom, p1)) {
                            endgame = true;
                            soundPlayer.pauseBackground();
                            soundPlayer.playMarioDies();
                            System.out.println("Game Over!");
                        }
                    }

                    //checks if end of game
                    if (collisionOccurs(flag, rightPlayerBox)) {
                        endgame = true;
                        soundPlayer.pauseBackground();
                        soundPlayer.playGameOver();
                        System.out.println("Game Over");
                    }
                } catch (java.lang.NullPointerException jlnpe) {

                }
            }
        }

        public Boolean checkBottomCollisions() {
            Boolean notGrounded = false;
            if (!p1.isJumping()) {
                notGrounded = true;
            }

            //checks if on the ground
            for (int i = 0; i < ground.size(); i++) {
                if (collisionOccurs(ground.elementAt(i), bottomPlayerBox)) {
                    notGrounded = false;
                    bottomCollision = true;
                }
            }

            //checks if on top of an object on the map
            for (int i = 0; i < map.size(); i++) {
                if (collisionOccurs(map.elementAt(i), bottomPlayerBox)) {
                    bottomCollision = true;
                    notGrounded = false;
                }
            }

            //checks if player is over a cliff
            for (int i = 0; i < cliff.size(); i++) {
                if (collisionOccurs(cliff.elementAt(i), bottomPlayerBox)) {
                    notGrounded = true;
                    bottomCollision = false;
                }
            }

            return notGrounded;
        }
    }

    private static class KeyPressed extends AbstractAction {
        public KeyPressed() {
            action = "";
        }
        public KeyPressed( String input ) {
            action = input;
        }

        public void actionPerformed( ActionEvent e ) {
            if (action.equals("UP")) {
                p1upPressed = true;
            }
            if (action.equals("DOWN")) {
                p1downPressed = true;
            }
            if (action.equals("LEFT")) {
                p1leftPressed = true;
            }
            if (action.equals("RIGHT")) {
                p1rightPressed = true;
            }
        }
        private String action;
    }

    private static class KeyReleased extends AbstractAction {
        public KeyReleased() {
            action = "";
        }
        public KeyReleased( String input ) {
            action = input;
        }

        public void actionPerformed( ActionEvent e ) {
            if (action.equals("UP")) {
                p1upPressed = false;
            }
            if (action.equals("DOWN")) {
                p1downPressed = false;
            }
            if (action.equals("LEFT")) {
                p1leftPressed = false;
            }
            if (action.equals("RIGHT")) {
                p1rightPressed = false;
            }
        }
        private String action;
    }

    private static class QuitGame implements ActionListener {
        public void actionPerformed( ActionEvent e ) {
            endgame = true;
            soundPlayer.pauseBackground();
        }
    }

    private static class StartGame implements ActionListener {
        public void actionPerformed( ActionEvent e ) {
            endgame = true;
            p1upPressed = false;
            p1downPressed = false;
            p1leftPressed = false;
            p1rightPressed = false;
            mushroomAlive = true;
            rightCollision = false;
            leftCollision = false;
            topCollision = false;
            bottomCollision = false;
            p1 = new ImageObject( p1originalX, p1originalY, p1width, p1height, -pi/2 );
            playerBox = new ImageObject((int) p1.getX() + 22, (int) p1.getY() + 20, (int) p1.getWidth(), (int) p1.getHeight(), 0);
            rightPlayerBox = new ImageObject((int) (p1.getX() + p1.getWidth() + 21), (int) p1.getY() + 20, 1, (int) p1.getHeight(), 0);
            leftPlayerBox = new ImageObject((int) (p1.getX() + 23), (int) p1.getY() + 20, 1, (int) p1.getHeight(), 0);
            topPlayerBox = new ImageObject((int) (p1.getX() + 22), (int) p1.getY() + 22, (int) p1.getWidth(), 1, 0);
            bottomPlayerBox = new ImageObject((int) (p1.getX() + 22), (int) (p1.getY() + 22 + p1.getHeight()), (int) p1.getWidth(), 1, 0);
            mushroom = new ImageObject(820, 395, goomba.getWidth(), goomba.getHeight(), 0);
            camera = new Camera();
            leftBounds = new ImageObject(0, 0, 11, WINHEIGHT, 0);
            outOfBounds = new ImageObject(0, 430, 6200, 10, 0);
            createMap();
            createGround();
            createCliff();
            flag = new ImageObject(6099, 0, 10, WINHEIGHT, 0);
            p1velocityX = 0;
            p1velocityY = 0;
            endgame = false;
            soundPlayer.restartBackground();
            try {
                Thread.sleep(50);
            }
            catch( InterruptedException ie ) {
            }
            Thread t1 = new Thread( new Animate() );
            Thread t2 = new Thread( new PlayerMover() );
            Thread t3 = new Thread( new GoombaMover() );
            Thread t4 = new Thread( new CollisionChecker() );
            t1.start();
            t2.start();
            t3.start();
            t4.start();
        }

        private static void createGround() {
            ImageObject bound1 = new ImageObject(0, 421, 2123, 6, 0);
            ImageObject bound2 = new ImageObject(2185, 421, 462, 6, 0);
            ImageObject bound3 = new ImageObject(2732, 421, 1970, 6, 0);
            ImageObject bound4 = new ImageObject(4765, 421, 1435, 6, 0);

            ground.addElement(bound1);
            ground.addElement(bound2);
            ground.addElement(bound3);
            ground.addElement(bound4);
        }

        private static void createCliff() {
            ImageObject bound1 = new ImageObject(2123, 421, 60, 6, 0);
            ImageObject bound2 = new ImageObject(2647, 421, 80, 6, 0);
            ImageObject bound3 = new ImageObject(4703, 421, 60, 6, 0);

            cliff.addElement(bound1);
            cliff.addElement(bound2);
            cliff.addElement(bound3);
        }

        private static void createMap() {
            ImageObject qbox1 = new ImageObject(492, 301, 30, 30, 0);
            ImageObject brick1 = new ImageObject(616,301,30,30, 0);
            ImageObject qbox2 = new ImageObject(646, 301, 30, 30, 0);
            ImageObject brick2 = new ImageObject(676, 301, 30, 30, 0);
            ImageObject qbox3 = new ImageObject(676,178,30,30, 0);
            ImageObject qbox4 = new ImageObject(706, 301, 30, 30, 0);
            ImageObject brick3 = new ImageObject(736, 301, 30, 30, 0);
            ImageObject tunnel1 = new ImageObject(861, 364, 61, 60, 0); //1
            ImageObject tunnel2 = new ImageObject(1170, 334, 61, 90, 0); //2
            ImageObject tunnel3 = new ImageObject(1417, 304, 61, 120, 0); //3
            ImageObject tunnel4 = new ImageObject(1755, 304, 61, 120, 0);

            ImageObject brick4 = new ImageObject(2369, 301, 30, 30, 0);
            ImageObject qbox5 = new ImageObject(2399, 301, 30, 30, 0);
            ImageObject brick5 = new ImageObject(2429, 301, 30, 30, 0);
            ImageObject brick6 = new ImageObject(2459, 178, 240, 30, 0);

            ImageObject brick7 = new ImageObject(2797, 178, 90, 30, 0);
            ImageObject qbox6 = new ImageObject(2887, 178, 30, 30, 0);
            ImageObject brick8 = new ImageObject(2887, 301, 30, 30, 0);
            ImageObject brick9 = new ImageObject(3077, 301, 30, 30, 0);
            ImageObject qbox7 = new ImageObject(3259, 301, 30, 30, 0);
            ImageObject qbox8 = new ImageObject(3349, 301, 30, 30, 0);
            ImageObject qbox9 = new ImageObject(3349, 178, 30, 30, 0);
            ImageObject qbox10 = new ImageObject(3445, 301, 30, 30, 0);
            ImageObject brick10 = new ImageObject(3627, 301, 30, 30, 0);
            ImageObject brick11 = new ImageObject(3719, 178, 90, 30, 0);
            ImageObject brick12 = new ImageObject(3935, 178, 30, 30, 0);
            ImageObject qbox11 = new ImageObject(3965, 178, 30, 30, 0);
            ImageObject qbox12 = new ImageObject(3995, 178, 30, 30, 0);
            ImageObject brick13 = new ImageObject(3965, 301, 60, 30, 0);
            ImageObject brick14 = new ImageObject(4025, 178, 30, 30, 0);
            ImageObject step1 = new ImageObject(4117, 391, 30, 30, 0); //1
            ImageObject step2 = new ImageObject(4137, 361, 30, 30, 0); //2
            ImageObject step3 = new ImageObject(4167, 331, 30, 30, 0); //3
            ImageObject step4 = new ImageObject(4197, 301, 30, 30, 0); //4
            ImageObject step5 = new ImageObject(4303, 301, 30, 30, 0);
            ImageObject step6 = new ImageObject(4333, 331, 30, 30, 0);
            ImageObject step7 = new ImageObject(4363, 361, 30, 30, 0);
            ImageObject step8 = new ImageObject(4393, 391, 30, 30, 0);

            ImageObject step9 = new ImageObject(4549, 391, 30, 30, 0);
            ImageObject step10 = new ImageObject(4579, 361, 30, 30, 0);
            ImageObject step11 = new ImageObject(4609, 331, 30, 30, 0);
            ImageObject step12 = new ImageObject(4639, 301, 30, 30, 0);
            ImageObject step13 = new ImageObject(4669, 301, 30, 30, 0);

            ImageObject step14 = new ImageObject(4763, 301, 30, 30, 0);
            ImageObject step15 = new ImageObject(4793, 331, 30, 30, 0);
            ImageObject step16 = new ImageObject(4823, 361, 30, 30, 0);
            ImageObject step17 = new ImageObject(4853, 391, 30, 30, 0);
            ImageObject tunnel5 = new ImageObject(5015, 364, 61, 60, 0);
            ImageObject brick15 = new ImageObject(5163, 301, 60, 30, 0);
            ImageObject qbox13 = new ImageObject(5223, 301, 30, 30, 0);
            ImageObject brick16 = new ImageObject(5253, 301, 30, 30, 0);
            ImageObject tunnel6 = new ImageObject(5503, 364, 61, 60, 0);
            ImageObject step18 = new ImageObject(5563, 391, 30, 30, 0);
            ImageObject step19 = new ImageObject(5593, 361, 30, 30, 0);
            ImageObject step20 = new ImageObject(5623, 331, 30, 30, 0);
            ImageObject step21 = new ImageObject(5653, 301, 30, 30, 0);
            ImageObject step22 = new ImageObject(5683, 271, 30, 30, 0);
            ImageObject step23 = new ImageObject(5713, 241, 30, 30, 0);
            ImageObject step24 = new ImageObject(5743, 211, 30, 30, 0);
            ImageObject step25 = new ImageObject(5773, 181, 30, 30, 0);
            ImageObject step26 = new ImageObject(5803, 181, 30, 30, 0);

            map.add(tunnel1);
            map.add(tunnel2);
            map.add(tunnel3);
            map.add(tunnel4);
            map.add(tunnel5);
            map.add(tunnel6);
            map.add(qbox1);
            map.add(qbox2);
            map.add(qbox3);
            map.add(qbox4);
            map.add(qbox5);
            map.add(qbox6);
            map.add(qbox7);
            map.add(qbox8);
            map.add(qbox9);
            map.add(qbox10);
            map.add(qbox11);
            map.add(qbox12);
            map.add(qbox13);
            map.add(brick1);
            map.add(brick2);
            map.add(brick3);
            map.add(brick4);
            map.add(brick5);
            map.add(brick6);
            map.add(brick7);
            map.add(brick8);
            map.add(brick9);
            map.add(brick10);
            map.add(brick11);
            map.add(brick12);
            map.add(brick13);
            map.add(brick14);
            map.add(brick15);
            map.add(brick16);
            map.add(step1);
            map.add(step2);
            map.add(step3);
            map.add(step4);
            map.add(step5);
            map.add(step6);
            map.add(step7);
            map.add(step8);
            map.add(step9);
            map.add(step10);
            map.add(step11);
            map.add(step12);
            map.add(step13);
            map.add(step14);
            map.add(step15);
            map.add(step16);
            map.add(step17);
            map.add(step18);
            map.add(step19);
            map.add(step20);
            map.add(step21);
            map.add(step22);
            map.add(step23);
            map.add(step24);
            map.add(step25);
            map.add(step26);
        }
    }

    private static Boolean isInside( double p1x, double p1y, double p2x1, double p2y1,
                                     double p2x2, double p2y2 ) {
        Boolean ret = false;
        if( p1x > p2x1 && p1x < p2x2 ) {
            if( p1y > p2y1 && p1y < p2y2 ) {
                ret = true;
            }
            if( p1y > p2y2 && p1y < p2y1 ) {
                ret = true;
            }
        }

        if( p1x > p2x2 && p1x < p2x1 ) {
            if( p1y > p2y1 && p1y < p2y2 ) {
                ret = true;
            }
            if( p1y > p2y2 && p1y < p2y1 ) {
                ret = true;
            }
        }
        return ret;
    }

    private static Boolean collisionOccursCoordinates( double p1x1, double p1y1, double
            p1x2, double p1y2, double p2x1, double p2y1, double p2x2, double p2y2 ) {
        Boolean ret = false;
        if( isInside( p1x1, p1y1, p2x1, p2y1, p2x2, p2y2 ) == true ) {
            ret = true;
        }
        if( isInside( p1x1, p1y2, p2x1, p2y1, p2x2, p2y2 ) == true ) {
            ret = true;
        }
        if( isInside( p1x2, p1y1, p2x1, p2y1, p2x2, p2y2 ) == true ) {
            ret = true;
        }
        if( isInside( p1x2, p1y2, p2x1, p2y1, p2x2, p2y2 ) == true ) {
            ret = true;
        }
        if( isInside( p2x1, p2y1, p1x1, p1y1, p1x2, p1y2 ) == true ) {
            ret = true;
        }
        if( isInside( p2x1, p2y2, p1x1, p1y1, p1x2, p1y2 ) == true ) {
            ret = true;
        }
        if( isInside( p2x2, p2y1, p1x1, p1y1, p1x2, p1y2 ) == true ) {
            ret = true;
        }
        if( isInside( p2x2, p2y2, p1x1, p1y1, p1x2, p1y2 ) == true ) {
            ret = true;
        }
        return ret;
    }

    private static Boolean collisionOccurs( ImageObject obj1, ImageObject obj2 ) {
        Boolean ret = false;
        if( collisionOccursCoordinates( obj1.getX(), obj1.getY(), obj1.getX() + obj1.getWidth(),
                obj1.getY() + obj1.getHeight(), obj2.getX(), obj2.getY(),
                obj2.getX() + obj2.getWidth(), obj2.getY() + obj2.getHeight() ) == true ) {
            ret = true;
        }
        return ret;
    }

    private static class ImageObject {
        public ImageObject() {
        }

        public ImageObject( double xinput, double yinput, double xwidthinput, double yheightinput, double angleinput) {
            x = xinput;
            y = yinput;
            xwidth = xwidthinput;
            yheight = yheightinput;
            angle = angleinput;
            internalangle = 0.0;

            velY = 0;
            jumping = false;
            falling = true;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getWidth() {
            return xwidth;
        }

        public double getHeight() {
            return yheight;
        }

        public double getAngle() {
            return angle;
        }

        public void setAngle( double angleinput ) {
            angle = angleinput;
        }

        public void move( double xinput, double yinput ) {
            x = x + xinput;
            y = y + yinput;
        }

        public void moveto( double xinput, double yinput ) {
            x = xinput;
            y = yinput;
        }

        public void rotate( double angleinput ) {
            angle = angle + angleinput;
            while( angle > twoPi ) {
                angle = angle - twoPi;
            }
            while( angle < 0 ) {
                angle = angle + twoPi;
            }
        }

        public void updateJump() {
            if (jumping && velY <= 0) {
                jumping = false;
                falling = true;
            } else if (jumping) {
                velY = velY - gravity;
                y = y - velY;
            }

            if (falling) {
                jumping = false;
                y = y + velY;
                velY = velY + gravity;
            }
        }

        public void jump() {
            if (!isJumping() && !isFalling()){
                jumping = true;
                velY = 11;
                soundPlayer.playJump();
            }
        }

        public boolean isJumping() {
            return jumping;
        }

        public boolean isFalling() {
            return falling;
        }

        private double x;
        private double y;
        private double xwidth;
        private double yheight;
        private double angle; // in Radians
        private double internalangle; // in Radians
        private boolean jumping;
        private boolean falling;
        private double velY;
    }

    private static void updatePlayerBox() {
        rightPlayerBox = new ImageObject((int) (p1.getX() + p1.getWidth() + 21 + camera.getX()),
                (int) p1.getY() + 40, 1, (int) p1.getHeight() - 40, 0);
        leftPlayerBox = new ImageObject((int) (p1.getX() + 23 + camera.getX()),
                (int) p1.getY() + 40, 1, (int) p1.getHeight() - 40, 0);
        topPlayerBox = new ImageObject((int) (p1.getX() + 32 + camera.getX()),
                (int) p1.getY() + 22, (int) p1.getWidth() / 2, 1, 0);
        bottomPlayerBox = new ImageObject((int) (p1.getX() + 35 + camera.getX()),
                (int) (p1.getY() + 15 + p1.getHeight()), (int) p1.getWidth() / 2, 1, 0);
    }

    private static void bindKey( JPanel myPanel, String input ) {
        myPanel.getInputMap(IFW).put(KeyStroke.getKeyStroke("pressed " + input), input + " pressed" );
        myPanel.getActionMap().put( input + " pressed", new KeyPressed( input ) );
        myPanel.getInputMap(IFW).put(KeyStroke.getKeyStroke("released " + input), input + " released" );
        myPanel.getActionMap().put(input + " released", new KeyReleased( input ) );
    }

    public static void main(String[] args) {
        setup();
        appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        appFrame.setSize(501,510);
        JPanel myPanel = new JPanel();
        lap = new String[] { "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten" };
        JComboBox<String> levelMenu = new JComboBox<String>( lap );
        levelMenu.setSelectedIndex(2);
        //levelMenu.addActionListener( new GameLap() );
        myPanel.add( levelMenu );
        JButton newGameButton = new JButton( "New Game" );
        newGameButton.addActionListener( new StartGame() );
        myPanel.add( newGameButton );
        JButton quitButton = new JButton( "Quit Game" );
        quitButton.addActionListener( new QuitGame() );
        myPanel.add( quitButton );
        bindKey( myPanel, "UP" );
        bindKey( myPanel, "DOWN" );
        bindKey( myPanel, "LEFT" );
        bindKey( myPanel, "RIGHT" );
        appFrame.getContentPane().add(myPanel, "South");
        appFrame.setVisible(true);
    }

    private static Boolean endgame;
    private static BufferedImage background;
    private static BufferedImage player1;
    private static BufferedImage goomba;
    private static Boolean p1upPressed;
    private static Boolean p1downPressed;
    private static Boolean p1leftPressed;
    private static Boolean p1rightPressed;
    private static Boolean mushroomAlive;
    private static ImageObject p1;
    private static ImageObject mushroom;
    private static double p1width;
    private static double p1height;
    private static double p1originalX;
    private static double p1originalY;
    private static int XOFFSET;
    private static int YOFFSET;
    private static int WINWIDTH;
    private static int WINHEIGHT;
    private static double pi;
    private static double twoPi;
    private static JFrame appFrame;
    private static final int IFW = JComponent.WHEN_IN_FOCUSED_WINDOW;
    private static String[] lap;
    private static Camera camera;
    private static double p1velocityX;
    private static double p1velocityY;
    private static double gravity;
    private static ImageObject leftBounds;
    private static Vector<ImageObject> ground;
    private static Vector<ImageObject> cliff;
    private static ImageObject flag;
    private static ImageObject playerBox;
    private static ImageObject rightPlayerBox;
    private static ImageObject leftPlayerBox;
    private static ImageObject topPlayerBox;
    private static ImageObject bottomPlayerBox;
    private static ImageObject outOfBounds;
    private static Vector<ImageObject> map;
    private static SoundPlayer soundPlayer;
    private static Boolean rightCollision;
    private static Boolean leftCollision;
    private static Boolean topCollision;
    private static Boolean bottomCollision;
}