package com.itfollows.game;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.media3.common.util.Log;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import com.itfollows.game.models.SnailSprite;

public class SnailSpriteAdapter extends RecyclerView.Adapter<SnailSpriteAdapter.ViewHolder> {

    private final Context context;
    private final List<SnailSprite> sprites;
    private String selectedIdentifier;
    private final OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(SnailSprite sprite);
    }

    public SnailSpriteAdapter(Context context, List<SnailSprite> sprites, String initialSelectedIdentifier, OnItemClickListener listener) {
        this.context = context;
        this.sprites = sprites;
        this.selectedIdentifier = initialSelectedIdentifier;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_snail_sprite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SnailSprite sprite = sprites.get(position);
        holder.bind(sprite);
    }

    @Override
    public int getItemCount() {
        return sprites.size();
    }

    public void setSelectedIdentifier(String identifier) {
        int oldSelectedPosition = -1;
        for (int i = 0; i < sprites.size(); i++) {
            if (sprites.get(i).identifier.equals(selectedIdentifier)) {
                oldSelectedPosition = i;
                break;
            }
        }

        this.selectedIdentifier = identifier;

        int newSelectedPosition = -1;
        for (int i = 0; i < sprites.size(); i++) {
            if (sprites.get(i).identifier.equals(selectedIdentifier)) {
                newSelectedPosition = i;
                break;
            }
        }

        if (oldSelectedPosition != -1) {
            notifyItemChanged(oldSelectedPosition);
        }
        if (newSelectedPosition != -1) {
            notifyItemChanged(newSelectedPosition);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView spriteImageView;
        TextView spriteNameTextView;
        ImageView selectionIndicator; // If using ImageView indicator
        LinearLayout itemLayout; // For border selection

        ViewHolder(View itemView) {
            super(itemView);
            spriteImageView = itemView.findViewById(R.id.spriteImageView);
            spriteNameTextView = itemView.findViewById(R.id.spriteNameTextView);
            selectionIndicator = itemView.findViewById(R.id.selectionIndicator);
            itemLayout = itemView.findViewById(R.id.spriteItemLayout);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    SnailSprite clickedSprite = sprites.get(position);

                    int oldSelectedAdapterPosition = -1;
                    for (int i = 0; i < sprites.size(); i++) {
                        if (sprites.get(i).identifier.equals(selectedIdentifier)) {
                            oldSelectedAdapterPosition = i;
                            break;
                        }
                    }

                    selectedIdentifier = clickedSprite.identifier;
                    Log.d("SettingsActivity_SpriteSelect", "Sprite selected in RecyclerView. Identifier: " + selectedIdentifier);

                    if (oldSelectedAdapterPosition != -1) {
                        notifyItemChanged(oldSelectedAdapterPosition); // Update old selection
                    }
                    notifyItemChanged(position); // Update new selection

                    onItemClickListener.onItemClick(clickedSprite);
                }
            });
        }

        void bind(SnailSprite sprite) {
            spriteNameTextView.setText(sprite.name);
            spriteImageView.setImageResource(sprite.thumbnailResId);

            if (sprite.identifier.equals(selectedIdentifier)) {
                selectionIndicator.setVisibility(View.VISIBLE);
                // itemLayout.setBackgroundResource(R.drawable.selected_sprite_border);
            } else {
                selectionIndicator.setVisibility(View.GONE);
                // itemLayout.setBackgroundResource(R.drawable.default_sprite_border);
            }
        }
    }
}