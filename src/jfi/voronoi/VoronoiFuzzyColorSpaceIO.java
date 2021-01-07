/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jfi.voronoi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jfi.color.fuzzy.FuzzyColorSpace;
import jfi.color.fuzzy.PolyhedralFuzzyColor;
import jfi.geometry.PlanarPolygon;
import jfi.geometry.Plane;
import jfi.geometry.Point3D;
import jfi.geometry.Polyhedron;

/**
 * Class which allows read and write fuzzy color spaces based on Voronoi
 * tessellation.
 * @author Míriam Mengíbar Rodríguez (mirismr@correo.ugr.es)
 */
public class VoronoiFuzzyColorSpaceIO {
    
    /**
     * Export a Voronoi Fuzzy Color Space. File structure is explained below.
     * @param fcs The Voronoi Fuzzy Color Space.
     * @param lambda lambda value used for scale kernel volumes.
     * @param filename Filename to export the Voronoi Fuzzy Color Space.
     * @return 
     */
    public static boolean writeFile(FuzzyColorSpace fcs, double lambda, String filename) {
        if (fcs == null) {
            return false;
        }

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            out.write("##################################################################################\r\n");
            out.write("#\r\n");
            out.write("#  FILE THAT CONTAINS CHECKED ATOMIC FUZZY COLOR SPACE with VORONOI PARTITION\r\n");
            out.write("#  YOU CAN EDIT THIS FILE ACCORDING TO THIS SPECIFICATION:\r\n");
            out.write("#      1- Color space name. It must start with @name\r\n");
            out.write("#      2- Color space description. It must start with @description\r\n");
            out.write("#      3- fuzzy color space type: It must start with @fuzzyColorSpaceType\r\n");
            out.write("#              int value. See static variables in FuzzyColorSpaceFileManager class\r\n");
            out.write("#      4- crisp color space type: It must start with @crispColorSpaceType\r\n");
            out.write("#              int value. See static variables in ColorSpaceJMR class\r\n");
            out.write("#      5-reference domain: It must start with @reference\r\n");
            out.write("#              minX \t maxX \t minY \t maxY \t minZ \t maxZ\r\n");
            out.write("#      6- lambda1 value: It must start with @lambda1.\r\n");
            out.write("#           Lambda1 is in [0,1] and corresponds with the core size. If lambda1 is 0.75, core size is a 0.75 fuzzy color volume size. If lambda1 is 1, core and support are equal than fuzzy color volume.\r\n");
            out.write("#      7- lambda2 value: It must start with @lambda2.\r\n");
            out.write("#           Lambda2 is in [1,2] and corresponds with the support size.\r\n");
            out.write("#      8- partition: It must start with @partition.\r\n");
            out.write("#              boolean value. \r\n");
            out.write("#      9- disjoint: It must start with @disjoint.\r\n");
            out.write("#              boolean value. \r\n");
            out.write("#      10- covering: It must start with @covering.\r\n");
            out.write("#              boolean value. \r\n");
            out.write("#      11- Number of samples in non-covering space: s. It must start with @samples\r\n");
            out.write("#              int value. \r\n");
            out.write("#      12- Number of colors in the fuzzy color space: c. It must start with @numberOfColors\r\n");
            out.write("#              int value. \r\n");
            out.write("#      13- For each fuzzy color 'fc':\r\n");
            out.write("#           13.1- label \\t X \\t Y \\t Z \\t boolean (label, representative point and boolean in the case of sampling color).\r\n");
            out.write("#           13.2- Number of negative prototypes for the fuzzy color 'fc': np. It must start with @numberOfNegatives\r\n");
            out.write("#                  int value. \r\n");
            out.write("#              for each negative prototype of 'fc':\r\n");
            out.write("#                   13.2.1- X \\t Y \\t Z \r\n");
            out.write("#           13.3- Number of faces for this color: f\r\n");
            out.write("#              for each face 'f': \r\n");
            out.write("#                13.3.1 (@Core)\r\n");
            out.write("#                  13.3.1.1- Plane in format A \\t B \\t C \\t D \r\n");
            out.write("#                  13.3.1.2- number of vertex for this face: v.\r\n");
            out.write("#                   for each vertex 'v':\r\n");
            out.write("#                     13.3.1.2.v- Vertex point in format X \\t Y \\t Z \r\n");
            out.write("#                13.3.2 (@Voronoi partition)\r\n");
            out.write("#                  13.3.2.1- Plane in format A \\t B \\t C \\t D \r\n");
            out.write("#                  13.3.2.2- number of vertex for this face: v.\r\n");
            out.write("#                   for each vertex 'v':\r\n");
            out.write("#                     13.3.2.2.v- Vertex point in format X \\t Y \\t Z \r\n");
            out.write("#                13.3.3 (@Support)\r\n");
            out.write("#                  13.3.3.1- Plane in format A \\t B \\t C \\t D \r\n");
            out.write("#                  13.3.3.2- number of vertex for this face: v.\r\n");
            out.write("#                   for each vertex 'v':\r\n");
            out.write("#                     13.3.3.2.v- Vertex point in format X \\t Y \\t Z \r\n");
            out.write("#\r\n");
            out.write("#\r\n");
            out.write("#  Created by SoTiLLo\r\n");
            out.write("#\r\n");
            out.write("##################################################################################\r\n");
            out.write("@nameFCS" + "\r\n");
            out.write("@descriptionPARTITION" + "\r\n");
            out.write("@fuzzyColorSpaceType0\r\n");
            out.write("@crispColorSpaceType1000\r\n");
            out.write("@reference0.0\t255.0\t0.0\t255.0\t0.0\t255.0\r\n");
            out.write("@lambda1" + lambda + "\r\n");
            out.write("@lambda2" + (2 - lambda) +"\r\n");
            out.write("@partitiontrue" + "\r\n");
            out.write("@disjointtrue" + "\r\n");
            out.write("@coveringtrue" + "\r\n");
            out.write("@samples500" + "\r\n");

            int numColors = fcs.size();
            out.write("@numberOfColors" + numColors + "\r\n");

            for (int i = 0; i < numColors; i++) {
                PolyhedralFuzzyColor fc = (PolyhedralFuzzyColor) fcs.get(i);

                out.write(fc.getLabel() + "\t" + fc.getPrototype().getX() + "\t" + fc.getPrototype().getY() + "\t" + fc.getPrototype().getZ() + "\t" + "false" + "\r\n"); // sample color false default
                // Negative propotypes
                ArrayList<Point3D> negatives = new ArrayList<Point3D>();
                int numNegatives = negatives.size();

                out.write("@numberOfNegatives" + numNegatives + "\r\n");
                for (int j = 0; j < numNegatives; j++) {
                    out.write(negatives.get(j).getX() + "\t" + negatives.get(j).getY() + "\t" + negatives.get(j).getZ() + "\r\n");
                }

                int numFaces = fc.getVolumes().get(0).getFaces().size();

                out.write(numFaces + "\r\n");

                for (int j = 0; j < numFaces; j++) {
                    //KERNEL
                    out.write("@core\r\n");
                    printFace(fc.getVolumes().get(0).getFaces().get(j), out);
                    //VORONOI REGION
                    out.write("@voronoi\r\n");
                    printFace(fc.getVolumes().get(1).getFaces().get(j), out);
                    //SUPPORT
                    out.write("@support\r\n");
                    printFace(fc.getVolumes().get(2).getFaces().get(j), out);
                }
            }
            out.close();
            System.out.println("Created Space File: " + filename);
            return true;
        } catch (IOException io) {
            System.err.println(io.getMessage());
            return false;
        }
    }
    
    /**
     * Write a face in a buffer.
     * @param f The hyperplane.
     * @param out The buffer to write on.
     */
    private static void printFace(PlanarPolygon f, BufferedWriter out) {
        try {
            out.write(f.getPlane().getOrthogonalVector().getCoordinates().getX() + "\t" + f.getPlane().getOrthogonalVector().getCoordinates().getY() + "\t" + f.getPlane().getOrthogonalVector().getCoordinates().getZ()
                    + "\t" + f.getPlane().getIndependentTerm() + "\t" + f.isOpen() + "\r\n");

            List<Point3D> vertex = f.getVertexSet();
            int numVertex = 0;
            if (vertex != null) {
                numVertex = vertex.size();
            }
            out.write(numVertex + "\r\n");
            for (int k = 0; k < numVertex; k++) {
                out.write(vertex.get(k).getX() + "\t" + vertex.get(k).getY() + "\t" + vertex.get(k).getZ() + "\r\n");
            }
        } catch (IOException io) {
            System.err.println(io.getMessage());
        }
    }

    /**
     * Read a file in .fcs format containing a set of voronoi fuzzy colors.
     *
     * @param file path to file
     * @return a set of voronoi fuzzy colors
     */
    public static FuzzyColorSpace readFile(String file) {
        String startName = "@name";
        String startFuzzyColorSpaceType = "@fuzzyColorSpaceType";
        String startLambda1 = "@lambda1";
        String startCrispColorSpaceType = "@crispColorSpaceType";
        String startNumberOfColors = "@numberOfColors";
        String startDescription = "@description";

        FuzzyColorSpace fuzzyColorsReaded = null;

        String s = "#";
        String[] spl;
        double rgb[] = new double[3];
        int numColors = 0;

        BufferedReader bf = null;
        try {
            bf = new BufferedReader(new FileReader(file));

            // skip comments that begin with the character '#'
            while (s != null && s.startsWith("#")) {
                s = bf.readLine();
            }

            // read color space name
            String colorSpaceName = (s.substring(startName.length()));

            // read description
            s = bf.readLine();
            String description = (s.substring(startDescription.length()));

            //read fuzzy color space type (atomic, hierarchical, ISCC, ISCC_BASIC, etc.)
            s = bf.readLine();
            int fuzzyColorSpaceType = Integer.parseInt(s.substring(startFuzzyColorSpaceType.length()));

            //read crisp color type
            s = bf.readLine();
            int crispColorSpaceType = Integer.parseInt(s.substring(startCrispColorSpaceType.length()));

            //read reference domain
            s = bf.readLine();
            //spl = s.split("\t", 0);
            //this.referenceDomain = new ReferenceDomain(Float.parseFloat(spl[0].substring(startReference.length())), Float.parseFloat(spl[1]), Float.parseFloat(spl[2]), Float.parseFloat(spl[3]), Float.parseFloat(spl[4]), Float.parseFloat(spl[5]));

            // read lambda1 value
            s = bf.readLine();
            Float lambda1 = Float.parseFloat(s.substring(startLambda1.length()));

            // read lambda2 value
            s = bf.readLine();
            //Float lambda2 = Float.parseFloat(s.substring(startLambda2.length()));

            // read partition boolean
            s = bf.readLine();
            boolean partition = false;
            if (s.substring("@partition".length()).compareTo("true") == 0) {
                partition = true;
            }

            // read disjoint boolean
            s = bf.readLine();
            boolean disjoint = false;
            if (s.substring("@disjoint".length()).compareTo("true") == 0) {
                disjoint = true;
            }

            // read covering boolean
            s = bf.readLine();
            boolean covering = false;
            if (s.substring("@covering".length()).compareTo("true") == 0) {
                covering = true;
            }
            // read the number of samples in noncovering spaces
            s = bf.readLine();
            //int samples = Integer.parseInt(s.substring(startSamples.length()));

            // read the number of colors
            s = bf.readLine();
            numColors = Integer.parseInt(s.substring(startNumberOfColors.length()));
            fuzzyColorsReaded = new FuzzyColorSpace();

            // read label and centroid values
            for (int i = 0; i < numColors; i++) {
                s = bf.readLine();
                spl = s.split("\t", 0);
                String colorName = spl[0];
                double r = Double.parseDouble(spl[1]);
                double g = Double.parseDouble(spl[2]);
                double b = Double.parseDouble(spl[3]);
                Point3D centroid = new Point3D(r, g, b);

                //read negative prototypes
                s = bf.readLine();

                // read the bounded voronoi regions
                List<PlanarPolygon> kernelHyperplanes = new ArrayList<>();
                List<PlanarPolygon> voronoiHyperplanes = new ArrayList<>();
                List<PlanarPolygon> supportHyperplanes = new ArrayList<>();

                s = bf.readLine();
                int numFaces = Integer.parseInt(s);
                for (int j = 0; j < numFaces; j++) {

                    Plane plane;
                    int numVertex;
                    boolean openPolygon;
                    //@core
                    s = bf.readLine();
                    s = bf.readLine();
                    spl = s.split("\t", 0);
                    if (spl[4].compareTo("true") == 0) {
                        openPolygon = true;
                    } else {
                        openPolygon = false;
                    }
                    // read plane coef to create it
                    // a1X + a2Y + a3Z + D
                    double a1 = Double.parseDouble(spl[0]);
                    double a2 = Double.parseDouble(spl[1]);
                    double a3 = Double.parseDouble(spl[2]);
                    plane = new Plane(a1, a2, a3, Double.parseDouble(spl[3]));
                    
                    s = bf.readLine();
                    // read hyperplane vertex
                    numVertex = Integer.parseInt(s);
                    List<Point3D> vertexSet = new ArrayList<>(numVertex);
                    for (int k = 0; k < numVertex; k++) {
                        s = bf.readLine();
                        spl = s.split("\t", 0);
                        vertexSet.add(new Point3D(Double.parseDouble(spl[0]), Double.parseDouble(spl[1]), Double.parseDouble(spl[2])));
                    }
                    kernelHyperplanes.add(new PlanarPolygon(plane, vertexSet, openPolygon));

                    //@voronoi
                    s = bf.readLine();
                    s = bf.readLine();
                    spl = s.split("\t", 0);
                    if (spl[4].compareTo("true") == 0) {
                        openPolygon = true;
                    } else {
                        openPolygon = false;
                    }
                    // read plane coef to create it
                    // a1X + a2Y + a3Z + D
                    a1 = Double.parseDouble(spl[0]);
                    a2 = Double.parseDouble(spl[1]);
                    a3 = Double.parseDouble(spl[2]);
                    plane = new Plane(a1, a2, a3, Double.parseDouble(spl[3]));
                    
                    s = bf.readLine();
                    // read hyperplane vertex set
                    numVertex = Integer.parseInt(s);
                    vertexSet = new ArrayList<>(numVertex);
                    for (int k = 0; k < numVertex; k++) {
                        s = bf.readLine();
                        spl = s.split("\t", 0);
                        vertexSet.add(new Point3D(Double.parseDouble(spl[0]), Double.parseDouble(spl[1]), Double.parseDouble(spl[2])));
                    }                  
                    voronoiHyperplanes.add(new PlanarPolygon(plane, vertexSet, openPolygon));

                    //@support
                    s = bf.readLine();
                    s = bf.readLine();
                    spl = s.split("\t", 0);
                    if (spl[4].compareTo("true") == 0) {
                        openPolygon = true;
                    } else {
                        openPolygon = false;
                    }

                    a1 = Double.parseDouble(spl[0]);
                    a2 = Double.parseDouble(spl[1]);
                    a3 = Double.parseDouble(spl[2]);
                    plane = new Plane(a1, a2, a3, Double.parseDouble(spl[3]));
                    
                    s = bf.readLine();
                    numVertex = Integer.parseInt(s);
                    vertexSet = new ArrayList<>(numVertex);
                    // read hyperplane vertex set
                    for (int k = 0; k < numVertex; k++) {
                        s = bf.readLine();
                        spl = s.split("\t", 0);
                        vertexSet.add(new Point3D(Double.parseDouble(spl[0]), Double.parseDouble(spl[1]), Double.parseDouble(spl[2])));
                    }
                    supportHyperplanes.add(new PlanarPolygon(plane, vertexSet, openPolygon));
                }
                try {
                    // build polytopes associates to this color
                    Polyhedron kernel = new Polyhedron(kernelHyperplanes);
                    kernel.setInnerPoint(centroid);
                    Polyhedron voronoi = new Polyhedron(voronoiHyperplanes);
                    voronoi.setInnerPoint(centroid);
                    Polyhedron support = new Polyhedron(supportHyperplanes);
                    support.setInnerPoint(centroid);

                    // build fuzzzy color with label, centroid and polytopes
                    PolyhedralFuzzyColor fuzzyColor = new PolyhedralFuzzyColor(colorName, centroid, kernel, voronoi, support);
                    fuzzyColorsReaded.add(fuzzyColor);
                } catch (Exception ex) {
                    Logger.getLogger(PolyhedralFuzzyColor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            bf.close();
        } catch (IOException io) {
            System.err.println(io.getMessage());
            return null;
        }

        return fuzzyColorsReaded;
    }
}
