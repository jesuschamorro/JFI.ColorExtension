/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jfi.voronoi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import jfi.color.ISCCColorMap;
import jfi.color.fuzzy.FuzzyColorSpace;
import jfi.color.fuzzy.GranularFuzzyColor;
import jfi.color.fuzzy.PolyhedralFuzzyColor;
import jfi.geometry.Plane;
import jfi.geometry.Line3D;
import jfi.geometry.PlanarPolygon;
import jfi.geometry.Point3D;
import jfi.geometry.Polyhedron;
import jfi.utils.Pair;

/**
 * Class which creates fuzzy color spaces based on Voronoi tessellation.
 *
 * @author Míriam Mengíbar Rodríguez (mirismr@correo.ugr.es)
 */
public class VoronoiFuzzyColorSpaceFactory {

    /**
     * Construct a new fuzzy color space based on a Voronoi tessellation
     *
     * @param centroids the centroids associates to each color.
     * @param labels the labels associates to each color.
     * @param lambda is in [0,1] and corresponds with the fuzzy color kernel
     * size. If lambda is 0.75, kernel size is a 0.75 fuzzy color volume size.
     * If lambda is 1, kernel and support are equal than fuzzy color volume. We
     * need this parameter for build support and kernel polytopes.
     * @return a new fuzzy color space based on a Voronoi tesselation.
     */
    public static FuzzyColorSpace createVoronoiFuzzyColorSpace(ArrayList<Point3D> centroids, ArrayList<String> labels, double lambda) {
        FuzzyColorSpace fcs = null;
        try {
            VoronoiTessellation3D voronoiTessellation = new VoronoiTessellation3D(centroids);
            fcs = createVoronoiFuzzyColorSpace(voronoiTessellation, labels, lambda);
        } catch (Exception ex) {
            Logger.getLogger(VoronoiFuzzyColorSpaceFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return fcs;
    }

    /**
     * Construct a new fuzzy color space based on a Voronoi tessellation
     *
     * @param colorMap Color centroids needed for building Voronoi Tesselation
     * @param lambda is in [0,1] and corresponds with the fuzzy color kernel
     * size. If lambda is 0.75, kernel size is a 0.75 fuzzy color volume size.
     * If lambda is 1, kernel and support are equal than fuzzy color volume. We
     * need this parameter for build support and kernel polytopes.
     * @return a new fuzzy color space based on a Voronoi tesselation.
     */
    public static FuzzyColorSpace createVoronoiFuzzyColorSpace(ISCCColorMap colorMap, double lambda) {
        FuzzyColorSpace fcs = null;
        try {
            VoronoiTessellation3D voronoiTessellation = new VoronoiTessellation3D(new ArrayList<>(colorMap.values()));
            fcs = createVoronoiFuzzyColorSpace(voronoiTessellation, new ArrayList<String>(colorMap.keySet()), lambda);
        } catch (Exception ex) {
            Logger.getLogger(VoronoiFuzzyColorSpaceFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return fcs;
    }

    /**
     * Construct a new fuzzy color space based on a Voronoi tessellation
     *
     * @param voronoiTessellation Voronoi tessellation.
     * @param labels the labels associates to each color in the fuzzy color
     * space.
     * @param lambda lambda is in [0,1] and corresponds with the fuzzy color
     * kernel size. If lambda is 0.75, kernel size is a 0.75 fuzzy color volume
     * size. If lambda is 1, kernel and support are equal than fuzzy color
     * volume. We need this parameter for build support and kernel polytopes.
     * @return a new fuzzy color space based on a Voronoi tesselation.
     */
    public static FuzzyColorSpace createVoronoiFuzzyColorSpace(VoronoiTessellation3D voronoiTessellation, ArrayList<String> labels, double lambda) {
        FuzzyColorSpace<Point3D> fcs = new FuzzyColorSpace<>();

        for (int i = 0; i < voronoiTessellation.getPoints().size(); i++) {
            Point3D centroid = voronoiTessellation.getPoints().get(i);
            Polyhedron volume = voronoiTessellation.getPolyhedrons().get(i);
            PolyhedralFuzzyColor vfc = getDefaultScaledPolyhedronFC(labels.get(i), volume, centroid, lambda);

            fcs.add(vfc);
        }

        // check points in kernel and support and fix them
        checkingPointsKernelSupport(fcs);

        return fcs;
    }

    /**
     * Creates a new Voronoi fuzzy color space based on voronoi membership
     * function. The idea underlying the procedure is to expand (i.e support
     * polytope) and reduce (i.e kernel polytope) the polytope associated to 0.5
     * alpha-cut.
     *
     * @param label the label of the Voronoi fuzzy color.
     * @param volume polytope associated to 0.5 alpha-cut.
     * @param centroid centroid point associated to fuzzy color.
     * @param lambda kernel scale factor. Support scale factor is calculated as
     * 2 - lambda.
     * @return a new Voronoi fuzzy color based on Voronoi membership functions.
     */
    static public PolyhedralFuzzyColor getDefaultScaledPolyhedronFC(String label, Polyhedron volume, Point3D centroid,
            double lambda) {
        PolyhedralFuzzyColor vfc = null;

        try {
            List<PlanarPolygon> kernelHyperPlanes = new ArrayList<>();
            List<PlanarPolygon> supportHyperPlanes = new ArrayList<>();

            // distance centroid to each polytope's face
            for (PlanarPolygon face : volume.getFaces()) {
                Plane hplane = face.getPlane();

                List<Point3D> positiveVertexSet = new ArrayList<Point3D>();
                List<Point3D> negativeVertexSet = new ArrayList<Point3D>();
                double dist = hplane.distanceToPoint(centroid) * (1 - lambda);

                // expand face negative and positive
                Plane positiveHplane = hplane.parallelPlane(dist);

                Plane negativeHplane = hplane.parallelPlane(-dist);

                List<Point3D> vertexSet = face.getVertexSet();
                if (vertexSet != null) {
                    // create vertex associates for the new hyperplanes
                    // only if the original hyperplane has vertex
                    for (Point3D v : vertexSet) {
                        Line3D centroidPoint = new Line3D(centroid, v);

                        Point3D intersectionPointPos = positiveHplane.getIntersectionPoint(centroidPoint);
                        positiveVertexSet.add(intersectionPointPos);

                        Point3D intersectionPointNeg = negativeHplane.getIntersectionPoint(centroidPoint);
                        negativeVertexSet.add(intersectionPointNeg);
                    }
                }

                PlanarPolygon positiveFace = new PlanarPolygon(positiveHplane, positiveVertexSet, face.isOpen());
                PlanarPolygon negativeFace = new PlanarPolygon(negativeHplane, negativeVertexSet, face.isOpen());

                double distToPositiveHplane = positiveHplane.distanceToPoint(centroid);
                double distToNegativeHplane = negativeHplane.distanceToPoint(centroid);

                if (distToPositiveHplane < distToNegativeHplane) {
                    kernelHyperPlanes.add(positiveFace);
                    supportHyperPlanes.add(negativeFace);
                } else {
                    kernelHyperPlanes.add(negativeFace);
                    supportHyperPlanes.add(positiveFace);
                }
            }

            Polyhedron kernelVolume = new Polyhedron(kernelHyperPlanes);
            kernelVolume.setInnerPoint(centroid);
            Polyhedron supportVolume = new Polyhedron(supportHyperPlanes);
            supportVolume.setInnerPoint(centroid);

            vfc = new PolyhedralFuzzyColor(label, centroid, kernelVolume, volume, supportVolume);

        } catch (Exception ex) {
            Logger.getLogger(PolyhedralFuzzyColor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return vfc;
    }

    /**
     * Creates a new granular fuzzy color based on Voronoi tessellation.
     *
     * @param label the granular fuzzy color label.
     * @param prototypes_p array of positive color prototypes.
     * @param prototypes_n array of negative color prototypes.
     * @param lambda kernel scale factor for each granule.
     * @return a granular fuzzy color based on Voronoi tessellation.
     */
    public static GranularFuzzyColor createVoronoiGranularFuzzyColor(String label,
            List<Point3D> prototypes_p, List<Point3D> prototypes_n, Double lambda) {

        List<Point3D> centroids = new ArrayList<Point3D>();
        centroids.addAll(prototypes_p);
        centroids.addAll(prototypes_n);

        // create polytopes associates to positives prototypes
        FuzzyColorSpace positivesColors = createSelectedFuzzyColors(centroids, prototypes_p, lambda);
        // create a granular fuzzy color with these voronoi fuzzy colors (based on positives polytopes)
        GranularFuzzyColor granularFuzzyColor = new GranularFuzzyColor(label);
        granularFuzzyColor.addAll(positivesColors);

        return granularFuzzyColor;
    }

    /**
     * Creates a new granular fuzzy color based on Voronoi tessellation.
     *
     * @param prototypes_p array of positive color prototypes.
     * @param prototypes_n array of negative color prototypes.
     * @param lambda kernel scale factor for each granule.
     * @return a granular fuzzy color based on Voronoi tessellation.
     */
    public static GranularFuzzyColor createVoronoiGranularFuzzyColor(List<Point3D> prototypes_p, List<Point3D> prototypes_n, Double lambda) {
        return createVoronoiGranularFuzzyColor("", prototypes_p, prototypes_n, lambda);
    }

    /**
     * Creates a new fuzzy color space based on Voronoi tessellations and
     * granular fuzzy colors.
     *
     * @param isccmap_positives Positives prototypes following ISCCColorMap
     * class
     * @param isccmap_negatives Negatives prototypes following ISCCColorMap
     * class
     * @param color_pattern_map Color pattern map for selecting a subset from
     * positives and negatives protypes
     * @return A new fuzzy color space based on Voronoi and granular fuzzy color
     */
    public static FuzzyColorSpace<Point3D> createVoronoiGranularFCS(
            ISCCColorMap isccmap_positives,
            ISCCColorMap isccmap_negatives,
            Map.Entry color_pattern_map[], Double lambda) {
        FuzzyColorSpace<Point3D> fcs = new FuzzyColorSpace();

        ExecutorService es = Executors.newCachedThreadPool();
        for (Map.Entry<String, Pair<String, String>> me : color_pattern_map) {
            es.execute(new Runnable() {
                public void run() {
                    ISCCColorMap map_positives = isccmap_positives.getSubset(me.getValue().getLeft());
                    List<Point3D> positives = new ArrayList<Point3D>(map_positives.values());

                    ISCCColorMap map_negatives = isccmap_negatives.getSubset(me.getValue().getRight());
                    List<Point3D> negatives = new ArrayList<Point3D>(map_negatives.values());

                    System.out.println("Creating granular color " + me.getKey());

                    GranularFuzzyColor granularFuzzyColor = createVoronoiGranularFuzzyColor(me.getKey(), positives, negatives, lambda);

                    // add granular to out
                    fcs.add(granularFuzzyColor);
                }
            });

        }

        // wait for threads
        es.shutdown();
        while (!es.isTerminated()) {
        }

        return fcs;
    }

    /**
     * Create a VoronoiFuzzyColorSpace only with a subset of colors. It creates
     * a Voronoi Tesselation based on all colors but only creates Voronoi Fuzzy
     * colors for a selected prototype subset.
     *
     * @param centroids Color centroids needed for building Voronoi Tesselation
     * @param selectedPrototypes The prototype subset
     * @param lambda is in [0,1] and corresponds with the fuzzy color kernel
     * size. If lambda is 0.75, kernel size is a 0.75 fuzzy color volume size.
     * If lambda is 1, kernel and support are equal than fuzzy color volume. We
     * need this parameter for build support and kernel polytopes
     * @return
     */
    private static FuzzyColorSpace createSelectedFuzzyColors(List<Point3D> centroids, List<Point3D> selectedPrototypes, double lambda) {
        FuzzyColorSpace<Point3D> fcs = new FuzzyColorSpace();
        try {
            VoronoiTessellation3D voronoiTessellation = new VoronoiTessellation3D(centroids);
            
            
            for (int i = 0; i < voronoiTessellation.getPoints().size(); i++) {
                Point3D centroid = voronoiTessellation.getPoints().get(i);
                if (selectedPrototypes.contains(centroid)) {
                    Polyhedron volume = voronoiTessellation.getPolyhedrons().get(i);
                    PolyhedralFuzzyColor vfc = getDefaultScaledPolyhedronFC("Color " + i, volume, centroid, lambda);
                    
                    fcs.add(vfc);
                }
                
            }
            // check points in kernel and support and fix them
            checkingPointsKernelSupport(fcs);
        } catch (Exception ex) {
            Logger.getLogger(VoronoiFuzzyColorSpaceFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return fcs;
    }

    /**
     * Check points in kernel and support and fix them for a fuzzy color space.
     *
     * @param fcs a fuzzy color space.
     */
    private static void checkingPointsKernelSupport(FuzzyColorSpace fcs) {
        PolyhedralFuzzyColor c1;
        PolyhedralFuzzyColor c2;
        // iterate throught all polytopes and check pairs i, j (i != j)
        try {
            for (int i = 0; i < fcs.size(); i++) {
                for (int j = 0; j < fcs.size(); j++) {
                    if (j != i) {
                        c1 = (PolyhedralFuzzyColor) fcs.get(i);
                        c2 = (PolyhedralFuzzyColor) fcs.get(j);

                        // only for colors with support and kernel created
                        if (c1.getVolumes().size() == 3) {
                            for (PlanarPolygon hpSupport : c1.getVolumes().get(2).getFaces()) {
                                List<Point3D> vi = hpSupport.getVertexSet();
                                if (vi != null) {
                                    for (int k = 0; k < vi.size(); k++) {
                                        Point3D vsupport = vi.get(k);
                                        Polyhedron c2Kernel = c2.getVolumes().get(0);
                                        if (c2Kernel.isPointInside(vsupport) && !c2Kernel.isPointInFace(vsupport)) {

                                            Point3D pFace = c2Kernel.getIntersectionPoint(new Line3D(vsupport, c1.getPrototype()));
                                            if (pFace != null) {
                                                // obtenemos los vértices antes de actualizar el hpsupport
                                                List<Point3D> vertexSupport = hpSupport.getVertexSet();
                                                //actualizamos la cara ... plano paralelo que pasa por punto pFace
                                                //hpSupport.modifyData(hpSupport.getPlane().getOrthogonalVector(), pFace, hpSupport.isInfinity());
                                                hpSupport.getPlane().modifyData(hpSupport.getPlane().getOrthogonalVector(), pFace);
                                                //actualizamos los vertex
                                                if (vertexSupport != null) {
                                                    List<Point3D> newVertexSet = new ArrayList<Point3D>();
                                                    //creamos nuevos vertices para cada cara del kernel y support
                                                    for (Point3D v : vertexSupport) {
                                                        Point3D intersectionPoint = hpSupport.getPlane().getIntersectionPoint(new Line3D(c1.getPrototype(), v));
                                                        newVertexSet.add(intersectionPoint);
                                                    }
                                                    hpSupport.setVertexSet(newVertexSet);
                                                }
                                                System.out.println(" -> [" + c1.getLabel() + "] support face moved");
                                            } else {
                                                System.err.println("NO fixed support point. Cannot move support face ");
                                            }
                                        }
                                    }
                                }
                            }
                            // for each hyperplane in c2 kernel
                            for (PlanarPolygon hpKernel : c2.getVolumes().get(0).getFaces()) {
                                List<Point3D> vi = hpKernel.getVertexSet();
                                if (vi != null) {
                                    for (int k = 0; k < vi.size(); k++) {
                                        Point3D vkernel = vi.get(k);
                                        Polyhedron c1Support = c1.getVolumes().get(2);
                                        if (c1Support.isPointInside(vkernel) && !c1Support.isPointInFace(vkernel)) {
                                            System.out.print("\tKernel point of [" + c2.getLabel() + "] inside support of [" + c1.getLabel() + "]");

                                            double minDist = Double.MAX_VALUE;
                                            PlanarPolygon nearestHp = null;
                                            for (PlanarPolygon hpSupport : c1Support.getFaces()) {
                                                double dist = hpSupport.getPlane().distanceToPoint(vkernel);
                                                if (dist < minDist) {
                                                    minDist = dist;
                                                    nearestHp = hpSupport;
                                                }
                                            }
                                            //actualizamos la cara mas cercana... plano paralelo que pasa por el vertice del kernel
                                            Plane newPlane = new Plane(nearestHp.getPlane().getOrthogonalVector(), vkernel);

                                            //Checking: testDist must be smaller than newDist
                                            double testDist = nearestHp.getPlane().distanceToPoint(c1.getPrototype());
                                            double newDist = newPlane.distanceToPoint(c1.getPrototype());
                                            if (testDist < newDist) {
                                                System.err.println("Error moving support face for [" + c1.getLabel() + "]. New support plane dist is not smaller");
                                            }
                                            // obtenemos los vértices antes de actualizar la cara mas cercana
                                            List<Point3D> vertexNearestHp = nearestHp.getVertexSet();
                                            
                                            nearestHp.getPlane().modifyData(nearestHp.getPlane().getOrthogonalVector(), vkernel);

                                            //actualizamos los vertex
                                            if (vertexNearestHp != null) {
                                                List<Point3D> newVertexSet = new ArrayList<Point3D>();
                                                //creamos nuevos vertices para cada cara del kernel y support
                                                for (Point3D v : vertexNearestHp) {
                                                    Point3D intersectionPoint = nearestHp.getPlane().getIntersectionPoint(new Line3D(c1.getPrototype(), v));
                                                    newVertexSet.add(intersectionPoint);
                                                }
                                                nearestHp.setVertexSet(newVertexSet);
                                            }
                                            System.out.println(" -> [" + c1.getLabel() + "] support face moved");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
