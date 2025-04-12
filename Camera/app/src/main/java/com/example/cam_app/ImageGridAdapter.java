package com.example.cam_app;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cam_app.databinding.GridItemImageBinding; // Import ViewBinding

import java.util.List;

public class ImageGridAdapter extends RecyclerView.Adapter<ImageGridAdapter.ImageViewHolder> {

    private List<ImageItem> images;
    private final OnItemClickListener onItemClickListener;

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(ImageItem imageItem);
    }

    public ImageGridAdapter(List<ImageItem> images, OnItemClickListener listener) {
        this.images = images;
        this.onItemClickListener = listener;
    }

    // Method to update data
    public void updateData(List<ImageItem> newImages) {
        this.images = newImages;
        notifyDataSetChanged(); // Consider using DiffUtil for better performance
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        GridItemImageBinding binding = GridItemImageBinding.inflate(inflater, parent, false);
        return new ImageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageItem currentItem = images.get(position);
        holder.bind(currentItem, onItemClickListener);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    // --- ViewHolder ---
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        private final GridItemImageBinding binding; // ViewBinding instance

        public ImageViewHolder(@NonNull GridItemImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final ImageItem imageItem, final OnItemClickListener listener) {
            Context context = binding.imageViewThumbnail.getContext();
            Glide.with(context)
                    .load(imageItem.getUri())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery) // Optional
                    .error(android.R.drawable.stat_notify_error)     // Optional
                    .into(binding.imageViewThumbnail);

            // Set click listener on the item view
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(imageItem);
                }
            });
        }
    }
}