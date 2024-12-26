import java.util.ArrayList;
import java.util.List;

class Face {
    private List<Integer> vertexIndices;   // Индексы вершин, образующих полигон

    public Face() {
        vertexIndices = new ArrayList<>();
    }

    public List<Integer> getVertexIndices() { return vertexIndices; }
}