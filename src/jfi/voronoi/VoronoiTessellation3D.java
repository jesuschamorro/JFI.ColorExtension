package jfi.voronoi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jfi.geometry.PlanarPolygon;
import jfi.geometry.Plane;
import jfi.geometry.Point3D;
import jfi.geometry.Polyhedron;
import jfi.geometry.Vector3D;

/**
 * Class representing a Voronoi tessellation on 3D space. It includes the
 * functionality of calculating the Voronoi tessellation in a 3D space given a
 * set of prototypes. This class depends of QHull Library
 * (http://www.qhull.org).
 *
 * @author Míriam Mengíbar Rodríguez (mirismr@correo.ugr.es)
 */
public class VoronoiTessellation3D {

    /*
    * Represents Voronoi cells which are polyhedrons in 3D spaces.
     */
    private List<Polyhedron> polyhedrons;

    /*
    * Represents the point set used for generating the Voronoi tessellation.
     */
    private List<Point3D> points;

    /*
    * Aux path for the qhull script generated.
     */
    private String voronoiScript;

    /*
    * Absolute path for the qhull executable.
     */
    private String voronoiExecutable;

    /*
    * Aux path for input file needed by qhull.
     */
    private String inVoronoi;

    /*
    * Aux path for output file where qhull write results.
     */
    private String outVoronoi;

    /*
    * Input file extension needed by qhull.
     */
    private final String fcvExtension = ".fcv";

    /*
    * Output file extension needed by qhull.
     */
    private final String fcpExtension = ".fcp";

    /**
     * Minimum point number neccesary for qhull library.
     */
    static final int QHULL_POINTS = 5;

    /**
     * Creates a new Voronoi tessellation on 3D space through a point set.
     *
     * @param points the point set.
     */
    public VoronoiTessellation3D(List<Point3D> points) {
        try {
            if (points.size() < QHULL_POINTS) {
                throw new InvalidParameterException("Points size given is " + points.size() + ". Minimum required size is "+QHULL_POINTS);
            }
            
            String voronoiScriptExtension = ".sh";
            String voronoiDependencyResource = "qvoronoi";
            if (isWindows()) {
                voronoiDependencyResource = "qvoronoi.exe";
                voronoiScriptExtension =  ".bat";
            }
            
            // obtain resource dependency absolute path
            this.voronoiExecutable = Paths.get(Thread.currentThread().getContextClassLoader().getResource(voronoiDependencyResource).toURI()).toFile().getAbsolutePath();
            
            // generate aux files as temp files
            this.outVoronoi = Files.createTempFile("outVoronoi"+ UUID.randomUUID(), fcvExtension).toAbsolutePath().toString();
            this.inVoronoi = Files.createTempFile("inVoronoi"+ UUID.randomUUID(), fcpExtension).toAbsolutePath().toString();
            this.voronoiScript = Files.createTempFile("voronoiScript"+UUID.randomUUID(), voronoiScriptExtension).toAbsolutePath().toString();
            
            this.polyhedrons = new ArrayList<Polyhedron>();
            this.points = points;
            
            // we need create a .fcp file with the points
            // needed for qhull library
            this.createInputPointsSetFile();
            
            // execute voronoi algorithm
            this.executeQHull();
            
            // read result
            this.readQhullResultsFile();
            
            // perform check for non-terminated voronoi cells
            this.checkNonTerminatedCells();
        } catch (URISyntaxException ex) {
            Logger.getLogger(VoronoiTessellation3D.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(VoronoiTessellation3D.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Create input file needed by qhull library. It contains the point set for
     * generating the Voronoi tesselation.
     *
     * @return true if input file is created or false otherwise
     */
    private boolean createInputPointsSetFile() {
        String pathFile = inVoronoi;

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(pathFile));
            out.write("3\r\n");
            out.write(this.points.size() + "\r\n");
            for (int i = 0; i < this.points.size(); i++) {
                out.write(this.points.get(i).x + "\t" + this.points.get(i).y + "\t" + this.points.get(i).z + "\r\n");
            }

            out.close();
        } catch (IOException io) {
            System.err.println(io.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Read results file generated by qhull library. It creates the polyhedrons
     * (i.e, the Voronoi cells on 3D spaces) conforming the Voronoi tesselation.
     *
     * @return true if the file is readed, false otherwise.
     */
    private boolean readQhullResultsFile() {
        String s = "#";
        String[] spl;
        int numColors = this.points.size();
        PlanarPolygon[][] faces;
        Plane[][] planes;
        boolean[][] openPolygons;

        BufferedReader bf = null;

        try {
            bf = new BufferedReader(new FileReader(outVoronoi));

            // create hyperplane matrix for storing faces
            // we need a matrix cause a hyperplane may belongs to two polyhedrons
            faces = new PlanarPolygon[numColors][numColors];
            planes = new Plane[numColors][numColors];
            openPolygons = new boolean[numColors][numColors];

            // read the bounded voronoi regions
            // not "infinity" regions
            s = bf.readLine();
            int numPlanes = Integer.parseInt(s);

            for (int i = 0; i < numPlanes; i++) {
                s = bf.readLine();
                spl = s.split(" ", 0);
                int index1 = Integer.parseInt(spl[1]);
                int index2 = Integer.parseInt(spl[2]);

                double[] planeInner = new double[4];
                int d = 3;
                for (int k = 0; k < 4; k++) {
                    while (spl[d].compareTo("") == 0) {
                        d++;
                    }
                    planeInner[k] = Double.parseDouble(spl[d++]);
                }

                Plane hp = new Plane(planeInner[0], planeInner[1], planeInner[2], planeInner[3]);

                openPolygons[index1][index2] = false;
                planes[index1][index2] = hp;
            }

            // read the unbounded voronoi regions
            s = bf.readLine();
            numPlanes = Integer.parseInt(s);
            for (int i = 0; i < numPlanes; i++) {
                s = bf.readLine();
                spl = s.split(" ", 0);
                int index1 = Integer.parseInt(spl[1]);
                int index2 = Integer.parseInt(spl[2]);

                double[] planeOutter = new double[4];
                int d = 3;
                for (int k = 0; k < 4; k++) {
                    while (spl[d].compareTo("") == 0) {
                        d++;
                    }
                    planeOutter[k] = Double.parseDouble(spl[d++]);
                }

                Plane hp = new Plane(planeOutter[0], planeOutter[1], planeOutter[2], planeOutter[3]);

                openPolygons[index1][index2] = true;
                planes[index1][index2] = hp;
            }

            // reading vertex coordinates
            s = bf.readLine(); // vertex dimension
            s = bf.readLine(); // number of vertex
            int numVertex = Integer.parseInt(s);
            Point3D[] vertex = new Point3D[numVertex];
            for (int i = 0; i < numVertex; i++) {
                s = bf.readLine();
                spl = s.split(" ", 0);
                int d = 0;
                double[] vertexPoint = new double[3];
                for (int k = 0; k < 3; k++) {
                    while (spl[d].compareTo("") == 0) {
                        d++;
                    }
                    vertexPoint[k] = Double.parseDouble(spl[d++]);
                }
                vertex[i] = new Point3D(vertexPoint[0], vertexPoint[1], vertexPoint[2]);
            }
            // reading vertex for each face
            s = bf.readLine(); // number of faces
            int numFaces = Integer.parseInt(s);
            for (int i = 0; i < numFaces; i++) {
                s = bf.readLine();
                spl = s.split(" ", 0);
                int index_i = Integer.parseInt(spl[1]);
                int index_j = Integer.parseInt(spl[2]);
                Plane plane = planes[index_i][index_j];
                boolean openPolygon = openPolygons[index_i][index_j];

                List<Point3D> vertexSet = new ArrayList<>();
                for (int j = 3; j <= Integer.parseInt(spl[0]); j++) {
                    int vertexIndex = Integer.parseInt(spl[j]);
                    if (vertexIndex == 0) {
                        openPolygon = true;
                    } else {
                        vertexSet.add(vertex[vertexIndex - 1]);
                    }
                }
                faces[index_i][index_j] = new PlanarPolygon(plane, vertexSet, openPolygon);
            }

            // we need create a list of faces for each centroid
            // rows will represent each centroid and columns the faces for each centroid
            ArrayList<ArrayList<PlanarPolygon>> hyperplanesForEachCentroid = new ArrayList<ArrayList<PlanarPolygon>>(numColors);
            for (int i = 0; i < numColors; i++) {
                hyperplanesForEachCentroid.add(new ArrayList<>());
            }

            for (int i = 0; i < numColors; i++) {
                for (int j = 0; j < numColors; j++) {
                    if (faces[i][j] != null) {
                        hyperplanesForEachCentroid.get(i).add(faces[i][j]);
                        hyperplanesForEachCentroid.get(j).add(faces[i][j]);
                    }
                }
            }

            // build a polytope for each centroid and add it to this.polyhedrons
            // cause we got inner point (centroid), set to each polytope
            for (int i = 0; i < hyperplanesForEachCentroid.size(); i++) {
                Polyhedron p = new Polyhedron(hyperplanesForEachCentroid.get(i));
                p.setInnerPoint(this.points.get(i));
                this.polyhedrons.add(p);
            }

            bf.close();

            // remove temporal files only when results are readed
            File delete = new File(outVoronoi);
            delete.delete();
            delete = new File(inVoronoi);
            delete.delete();
            delete = new File(this.voronoiScript);
            delete.delete();
        } catch (IOException io) {
            System.err.println(io.getMessage());
            return false;
        } catch (Exception ex) {
            Logger.getLogger(VoronoiTessellation3D.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    /**
     * Check if the Voronoi cells are well constructed and fix them if is
     * neccesary.
     */
    private void checkNonTerminatedCells() {
        for (int i = 0; i <= 255; i++) {
            for (int j = 0; j <= 255; j++) {
                for (int k = 0; k <= 255; k++) {
                    // for each point in space, 
                    Point3D xyz = new Point3D(i, j, k);

                    //check if it is inside of two distinct voronoi polyhedrons v1 and v2
                    for (int x = 0; x < this.polyhedrons.size(); x++) {
                        try {
                            Polyhedron v1 = this.polyhedrons.get(x);
                            if (v1.isPointInside(xyz) && !v1.isPointInFace(xyz)) {
                                for (int y = 0; y < this.polyhedrons.size(); y++) {
                                    if (y != x) {
                                        Polyhedron v2 = this.polyhedrons.get(y);
                                        if (v2.isPointInside(xyz) && !v2.isPointInFace(xyz)) {

                                            // create a perpendicular plane to vector from one polytope points to another one
                                            // and the mid point between those points
                                            Point3D v1Centroid = v1.getInnerPoint();
                                            Point3D v2Centroid = v2.getInnerPoint();
                                            Vector3D vector = new Vector3D(v2Centroid, v1Centroid);
                                            Point3D v1v2MiddlePoint = middlePoint(v1Centroid, v2Centroid);

                                            Plane perpendicularPlane = Plane.perpendicularPlane(vector, v1v2MiddlePoint);

                                            // add the faces to two polyhedrons
                                            List<PlanarPolygon> faces = v1.getFaces();
                                            // planar polygon without vertex
                                            faces.add(new PlanarPolygon(perpendicularPlane, new ArrayList<Point3D>(), true));
                                            v1.setFaces(faces);

                                            faces = v2.getFaces();
                                            // planar polygon without vertex
                                            faces.add(new PlanarPolygon(perpendicularPlane, new ArrayList<Point3D>(), true));
                                            v2.setFaces(faces);
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(VoronoiTessellation3D.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculate the middle point between p1 and p2.
     *
     * @param p1 one point
     * @param p2 another point
     * @return the middle point between pi and p2
     */
    private Point3D middlePoint(Point3D p1, Point3D p2) {
        return new Point3D((p1.x + p2.x) / 2.0, (p1.y + p2.y) / 2.0, (p1.z + p2.z) / 2.0);
    }

    /**
     * Returns the polyhedron set conforming the Voronoi tesellation.
     *
     * @return the polyhedron set conforming the Voronoi tesellation.
     */
    public List<Polyhedron> getPolyhedrons() {
        return polyhedrons;
    }

    /**
     * Returns the point set used for generating the Voronoi tesellation.
     *
     * @return the point set used for generating the Voronoi tesellation.
     */
    public List<Point3D> getPoints() {
        return points;
    }

    /**
     * Create the aux script file for executing qhull on Windows system.
     */
    private void createWinBatFile() {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(voronoiScript));
            out.write(voronoiExecutable + " Fi Fo p Fv <" + inVoronoi + " > " + outVoronoi + "\r\n");
            out.close();
        } catch (Exception e) {
            // Exception if exec fails
            e.printStackTrace();
        }
    }

    /**
     * Create the aux script file for executing qhull on linux system.
     */
    private void createLinuxBatFile() {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(voronoiScript));
            out.write("#!/bin/bash \n");
            out.write(voronoiExecutable + " Fi Fo p Fv <" + inVoronoi + " > " + outVoronoi);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the aux script file for executing qhull on osx system.
     */
    private void createMacBatFile() {
        createLinuxBatFile();
    }

    /**
     * Check if system used is Windows.
     *
     * @return true if system used is Windows, false otherwise
     */
    private boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        // windows
        return (os.indexOf("win") >= 0);

    }

    /**
     * Check if system used is osx.
     *
     * @return true if system used is osx, false otherwise
     */
    private boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        // Mac
        return (os.indexOf("mac") >= 0);

    }

    /**
     * Check if system used is Linux.
     *
     * @return true if system used is Linux, false otherwise
     */
    private boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        // linux or unix
        return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);

    }

    /**
     * Execute the qhull script created.
     *
     * @return true if the script is executed without problems, false otherwise.
     */
    private boolean executeQHull() {
        try {
            if (isWindows()) {
                createWinBatFile();
            } else if (isLinux()) {
                createLinuxBatFile();
            } else if (isMac()) {
                createMacBatFile();
            } else {
                createWinBatFile();
            }

            if (isLinux() || isMac()) {
                Runtime.getRuntime().exec("chmod +x " + this.voronoiExecutable).waitFor();

                Runtime.getRuntime().exec("chmod +x " + this.voronoiScript).waitFor();
            } 
            
            Runtime.getRuntime().exec(this.voronoiScript).waitFor();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(VoronoiTessellation3D.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(VoronoiTessellation3D.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(VoronoiTessellation3D.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }
}
