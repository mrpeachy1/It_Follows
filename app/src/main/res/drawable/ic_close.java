ImageButton closeBtn = findViewById(R.id.closeInventoryBtn);
RelativeLayout inventoryPanel = findViewById(R.id.inventoryPanel);

closeBtn.setOnClickListener(v -> inventoryPanel.setVisibility(View.GONE));