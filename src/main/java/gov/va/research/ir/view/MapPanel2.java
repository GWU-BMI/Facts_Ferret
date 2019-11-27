package gov.va.research.ir.view;

import com.sun.javafx.application.PlatformImpl;
import com.vividsolutions.jts.geom.Coordinate;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.geotools.feature.SchemaException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MapPanel2  extends AbstractMapPanel {

    private Stage stage;
    private WebView browser;
    private JFXPanel jfxPanel;
    private JButton swingButton;
    private WebEngine webEngine;
    private String jsscripts[] = new String[] { null , null };
    private boolean done = false;


    public MapPanel2() {
        initComponents();
    }
    private void initComponents(){

        jfxPanel = new JFXPanel();
        createScene();

        setLayout(new BorderLayout());
        add(jfxPanel, BorderLayout.CENTER);
    }
    public void setup() {
        jsscripts[0] = jsscripts[1] = null;
        done = false;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                webEngine.reload();
            }
        });
    }

    public void updateMap(final Map<Coordinate, Integer> coordinateSubtotalMap) throws IOException,
            SchemaException, SQLException, ClassNotFoundException,
            URISyntaxException {
        int     value[] = new int[9];
        for ( Coordinate c : coordinateSubtotalMap.keySet() ) {
            value[(int)c.x - 1] = coordinateSubtotalMap.get(c);
        }
        StringBuffer sb = new StringBuffer();
        for ( int i=0;i<9;++i ) {
            sb.append(value[i]).append(',');
        }
        sb.deleteCharAt(sb.length()-1);
        jsscripts[0] = "updateData([" + sb.toString() + "])";
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                if ( jsscripts[0] != null )
                    webEngine.executeScript(jsscripts[0]);
            }
        });

    }

    public void updateHp(final Map<String,Integer> hospital_data) {
        StringBuffer  sb = new StringBuffer();
        for ( String name : hospital_data.keySet() ) {
            sb.append(name).append("|").append(hospital_data.get(name)).append(";");
        }
        sb.deleteCharAt(sb.length()-1);
        jsscripts[1] = "updateHp(\"" + sb.toString() + "\")";
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                if ( jsscripts[1] != null )
                    webEngine.executeScript(jsscripts[1]);
            }
        });
    }
    @Override
    public void updatePanel() {
//		Platform.runLater(new Runnable() {
//
//			@Override
//			public void run() {
//				if ( jsscripts[0] != null )
//					webEngine.executeScript(jsscripts[0]);
//				if ( jsscripts[1] != null )
//					webEngine.executeScript(jsscripts[1]);
//			}
//		});

    }
    /**
     * createScene
     *
     * Note: Key is that Scene needs to be created and run on "FX user thread"
     *       NOT on the AWT-EventQueue Thread
     *
     */
    private void createScene() {
        PlatformImpl.startup(new Runnable() {
            @Override
            public void run() {

                browser = new WebView();
                webEngine = browser.getEngine();
                webEngine.load( "http://34.200.0.24/states_21basic.html" );
                browser.setPrefSize(1050,800);
                jfxPanel.setScene( new Scene( browser ) );
//				stage = new Stage();
//
//				stage.setTitle("Hello Java FX");
//				stage.setResizable(true);
//
//				Group root = new Group();
//				Scene scene = new Scene(root,80,20);
//				stage.setScene(scene);
//
//				// Set up the embedded browser:
//				browser = new WebView();
//				browser.setPrefSize(1050,800);
//				browser.setMaxHeight(2000);
//
//
//				webEngine = browser.getEngine();
//				webEngine.load("http://34.200.0.24/states_21basic.html");
//
//				ObservableList<Node> children = root.getChildren();
//				children.add(browser);
//
//				jfxPanel.setScene(scene);

            }
        });
    }


    @Override
    public List<PDPage> addPdfPages(PDDocument pdDocument) throws IOException {
        return null;
    }

    @Override
    public void reset() {
        setup();
    }

    @Override
    public void dispose() {

    }
}
