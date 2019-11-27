package gov.va.research.ir.view;

import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.feature.SchemaException;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Map;

public abstract class AbstractMapPanel extends JPanel implements PdfExportable {
    public abstract void reset();
    public abstract void dispose();
    public abstract void updateMap(final Map<Coordinate, Integer> coordinateSubtotalMap) throws IOException,
            SchemaException, SQLException, ClassNotFoundException,
            URISyntaxException;
    public void updateHp(final Map<String,Integer> hospital_data) {
    }
    public void updatePanel() {

    }
}