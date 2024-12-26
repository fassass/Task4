import java.util.ArrayList;
import java.util.List;

// Класс для хранения 3D модели
class Model3D {
    private List<Vector3> vertices;    // Список вершин модели
    private List<Face> polygons;       // Список полигонов модели

    public Model3D() {
        vertices = new ArrayList<>();
        polygons = new ArrayList<>();
    }

    public List<Vector3> getVertices() { return vertices; }
    public List<Face> getPolygons() { return polygons; }
}