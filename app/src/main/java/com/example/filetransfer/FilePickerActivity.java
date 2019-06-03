package com.example.filetransfer;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FilePickerActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_FILE_PICK = 1000;

    private TextView tv_path;
    private RecyclerView rv_file;
    private TextView tv_back;

    private FileAdapter fileAdapter;
    private List<File> files;
    private String path;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filepicker);

        tv_path = findViewById(R.id.tv_path);
        rv_file = findViewById(R.id.rv_file);
        tv_back = findViewById(R.id.tv_back);

        if (!checkSdState()) {
            Toast.makeText(getApplicationContext(), "sdcard not mounted", Toast.LENGTH_SHORT).show();
            return;
        }

        path = Environment.getExternalStorageDirectory().getPath();
        tv_path.setText(path);
        files = getFileList(path);
        fileAdapter = new FileAdapter(files, this);
        rv_file.setLayoutManager(new LinearLayoutManager(this));
        rv_file.setAdapter(fileAdapter);

        tv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tempPath = new File(path).getParent();
                if (tempPath == null) {
                    return;
                }
                path = tempPath;
                files = getFileList(path);
                fileAdapter.setData(files);
                tv_path.setText(path);
            }
        });

        fileAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                File file = files.get(position);
                if (file.isDirectory()) {
                    path = file.getAbsolutePath();
                    tv_path.setText(path);
                    files = getFileList(path);
                    fileAdapter.setData(files);
                } else {
                    System.out.println(file.getAbsolutePath());
                }
            }
        });
    }


    private boolean checkSdState() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private static List<File> getFileList(String path) {
        File dir = new File(path);
        File[] files = dir.listFiles();
        List<File> result = new ArrayList<>();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                result.add(files[i]);
            }
            Collections.sort(result, new FileComparator());
        }

        return result;
    }

    private static String getReadableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private static class FileComparator implements Comparator<File> {

        @Override
        public int compare(File f1, File f2) {
            if(f1 == f2) {
                return 0;
            }
            if(f1.isDirectory() && f2.isFile()) {
                // Show directories above files
                return -1;
            }
            if(f1.isFile() && f2.isDirectory()) {
                // Show files below directories
                return 1;
            }
            if (f1.getName().equals(f2.getName())) {
                return 0;
            }
            return f1.getName().compareTo(f2.getName());
        }
    }

    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileHolder>{
        private List<File> files;
        private Context context;
        private OnItemClickListener onItemClickListener;

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }

        public FileAdapter(List<File> files, Context context) {
            this.files = files;
            this.context = context;
        }

        @NonNull
        @Override
        public FileHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = android.view.View.inflate(context, R.layout.item_file, null);
            FileHolder fileHolder = new FileHolder(view);
            return fileHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull FileHolder fileHolder, final int i) {
            File file = files.get(i);
            if (file.isFile()) {
                fileHolder.iv_file_icon.setBackgroundResource(R.mipmap.lfile_file_style_yellow);
                fileHolder.tv_file_name.setText(file.getName());
                fileHolder.tv_file_detail.setText("size:" + getReadableFileSize(file.length()));
            } else {
                fileHolder.iv_file_icon.setBackgroundResource(R.mipmap.lfile_folder_style_yellow);
                fileHolder.tv_file_name.setText(file.getName());
                List<File> subDirFiles = getFileList(file.getAbsolutePath());
                if (subDirFiles == null) {
                    fileHolder.tv_file_detail.setText("0 item");
                } else {
                    fileHolder.tv_file_detail.setText(subDirFiles.size() + " item");
                }
            }
            fileHolder.rl_file_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(i);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        public void setData(List<File> files) {
            this.files = files;
            notifyDataSetChanged();
        }

        class FileHolder extends RecyclerView.ViewHolder{
            public RelativeLayout rl_file_item;
            public ImageView iv_file_icon;
            public TextView tv_file_name;
            public TextView tv_file_detail;

            public FileHolder(@NonNull View itemView) {
                super(itemView);
                rl_file_item = itemView.findViewById(R.id.rl_file_item);
                iv_file_icon = itemView.findViewById(R.id.iv_file_icon);
                tv_file_name = itemView.findViewById(R.id.tv_file_name);
                tv_file_detail = itemView.findViewById(R.id.tv_file_detail);
            }
        }
    }

}
