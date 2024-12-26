import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

class ObjWriter {
    public static void write(Model3D model, File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        // Добавляем комментарий с информацией
        writer.write("# Exported from ModelViewer\n");
        writer.write("# Vertices: " + model.getVertices().size() + "\n");
        writer.write("# Faces: " + model.getPolygons().size() + "\n\n");

        // Записываем вершины с высокой точностью
        DecimalFormat df = new DecimalFormat("0.######");
        df.setDecimalSeparatorAlwaysShown(false);

        for (Vector3 vertex : model.getVertices()) {
            writer.write(String.format("v %s %s %s\n",
                    df.format(vertex.x),
                    df.format(vertex.y),
                    df.format(vertex.z)));
        }

        writer.write("\n");

        // Записываем полигоны с текстурными координатами и нормалями
        for (Face face : model.getPolygons()) {
            writer.write("f");
            for (Integer index : face.getVertexIndices()) {
                // Формат: вершина/текстура/нормаль
                writer.write(" " + (index + 1) + "//");
            }
            writer.write("\n");
        }

        writer.close();
    }
}