package Objects;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class Contact {
    private int id;
    private String name;
    private Bitmap profile_image;

    public Contact(int id, String name, Bitmap profile_image) {
        this.id = id;
        this.name = name;
        this.profile_image = profile_image;
    }
    //Constructor para grupos, se trata como a un contacto pero sin imagen
    public Contact(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Bitmap getProfile_image() {
        return profile_image;
    }

    public void setProfile_image(Bitmap profile_image) {
        this.profile_image = profile_image;
    }

}
