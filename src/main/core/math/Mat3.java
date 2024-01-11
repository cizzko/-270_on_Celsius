package core.math;

// column major matrix
public final class Mat3 {
    public static final int M00 = 0;
    public static final int M01 = 3;
    public static final int M02 = 6;

    public static final int M10 = 1;
    public static final int M11 = 4;
    public static final int M12 = 7;

    public static final int M20 = 2;
    public static final int M21 = 5;
    public static final int M22 = 8;

    public final float[] val = new float[9];

    public Mat3() {
        identity();
    }

    public Mat3(Mat3 matrix) {
        set(matrix);
    }

    public Mat3(float[] values) {
        set(values);
    }

    private void mul(float m00, float m01, float m02,
                     float m10, float m11, float m12,
                     float m20, float m21, float m22) {

        float[] val = this.val;

        float v00 = val[M00] * m00 + val[M01] * m10 + val[M02] * m20;
        float v01 = val[M00] * m01 + val[M01] * m11 + val[M02] * m21;
        float v02 = val[M00] * m02 + val[M01] * m12 + val[M02] * m22;

        float v10 = val[M10] * m00 + val[M11] * m10 + val[M12] * m20;
        float v11 = val[M10] * m01 + val[M11] * m11 + val[M12] * m21;
        float v12 = val[M10] * m02 + val[M11] * m12 + val[M12] * m22;

        float v20 = val[M20] * m00 + val[M21] * m10 + val[M22] * m20;
        float v21 = val[M20] * m01 + val[M21] * m11 + val[M22] * m21;
        float v22 = val[M20] * m02 + val[M21] * m12 + val[M22] * m22;

        val[M00] = v00;
        val[M10] = v10;
        val[M20] = v20;
        val[M01] = v01;
        val[M11] = v11;
        val[M21] = v21;
        val[M02] = v02;
        val[M12] = v12;
        val[M22] = v22;
    }

    public Mat3 setOrthographic(float x, float y, float width, float height) {
        float right = x + width, top = y + height;

        float xOrth = 2 / (right - x);
        float yOrth = 2 / (top - y);

        float tx = -(right + x) / (right - x);
        float ty = -(top + y) / (top - y);

        val[M00] = xOrth;
        val[M11] = yOrth;

        val[M02] = tx;
        val[M12] = ty;
        val[M22] = 1f;

        return this;
    }

    public Mat3 identity() {
        float[] val = this.val;
        val[M00] = 1;
        val[M10] = 0;
        val[M20] = 0;
        val[M01] = 0;
        val[M11] = 1;
        val[M21] = 0;
        val[M02] = 0;
        val[M12] = 0;
        val[M22] = 1;
        return this;
    }

    public Mat3 mul(Mat3 m) {
        float[] val = this.val;
        float[] val2 = m.val;

        float v00 = val[M00] * val2[M00] + val[M01] * val2[M10] + val[M02] * val2[M20];
        float v01 = val[M00] * val2[M01] + val[M01] * val2[M11] + val[M02] * val2[M21];
        float v02 = val[M00] * val2[M02] + val[M01] * val2[M12] + val[M02] * val2[M22];

        float v10 = val[M10] * val2[M00] + val[M11] * val2[M10] + val[M12] * val2[M20];
        float v11 = val[M10] * val2[M01] + val[M11] * val2[M11] + val[M12] * val2[M21];
        float v12 = val[M10] * val2[M02] + val[M11] * val2[M12] + val[M12] * val2[M22];

        float v20 = val[M20] * val2[M00] + val[M21] * val2[M10] + val[M22] * val2[M20];
        float v21 = val[M20] * val2[M01] + val[M21] * val2[M11] + val[M22] * val2[M21];
        float v22 = val[M20] * val2[M02] + val[M21] * val2[M12] + val[M22] * val2[M22];

        val[M00] = v00;
        val[M10] = v10;
        val[M20] = v20;
        val[M01] = v01;
        val[M11] = v11;
        val[M21] = v21;
        val[M02] = v02;
        val[M12] = v12;
        val[M22] = v22;

        return this;
    }

    public Mat3 setToTranslation(float x, float y) {
        float[] val = this.val;

        val[M00] = 1;
        val[M10] = 0;
        val[M20] = 0;

        val[M01] = 0;
        val[M11] = 1;
        val[M21] = 0;

        val[M02] = x;
        val[M12] = y;
        val[M22] = 1;

        return this;
    }

    public float det() {
        float[] val = this.val;
        return val[M00] * val[M11] * val[M22] +
               val[M01] * val[M12] * val[M20] +
               val[M02] * val[M10] * val[M21] -
               val[M00] * val[M12] * val[M21] -
               val[M01] * val[M10] * val[M22] -
               val[M02] * val[M11] * val[M20];
    }

    public Mat3 inv() {
        float det = det();
        if (det == 0) {
            throw new RuntimeException("Can't invert a singular matrix");
        }

        float invDet = 1f / det;
        float[] val = this.val;

        float m00 = val[M11] * val[M22] - val[M21] * val[M12];
        float m10 = val[M20] * val[M12] - val[M10] * val[M22];
        float m20 = val[M10] * val[M21] - val[M20] * val[M11];
        float m01 = val[M21] * val[M02] - val[M01] * val[M22];
        float m11 = val[M00] * val[M22] - val[M20] * val[M02];
        float m21 = val[M20] * val[M01] - val[M00] * val[M21];
        float m02 = val[M01] * val[M12] - val[M11] * val[M02];
        float m12 = val[M10] * val[M02] - val[M00] * val[M12];
        float m22 = val[M00] * val[M11] - val[M10] * val[M01];

        val[M00] = invDet * m00;
        val[M10] = invDet * m10;
        val[M20] = invDet * m20;
        val[M01] = invDet * m01;
        val[M11] = invDet * m11;
        val[M21] = invDet * m21;
        val[M02] = invDet * m02;
        val[M12] = invDet * m12;
        val[M22] = invDet * m22;

        return this;
    }

    public Mat3 set(Mat3 mat) {
        System.arraycopy(mat.val, 0, val, 0, val.length);
        return this;
    }

    public Mat3 set(float[] values) {
        System.arraycopy(values, 0, val, 0, val.length);
        return this;
    }

    public Mat3 translate(float x, float y) {
        mul(1, 0, 0,
                0, 1, 0,
                x, y, 1);
        return this;
    }

    public Mat3 translate(Vector2f translation) {
        return translate(translation.x, translation.y);
    }

    public Mat3 scale(float scale) {
        val[M00] *= scale;
        val[M11] *= scale;
        return this;
    }
}
