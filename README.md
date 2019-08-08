# Smart Recycler View

Smart Recycler View is an android library project to help developers to use the recycler view implementation in an easy way.

## Dependencies
* androidx.recyclerview:recyclerview:1.1.0-beta01
* androidx.legacy:legacy-support-core-utils:1.0.0

## Set up Instructions
Set up the project dependencies. To use this library in your project:

(1) Use the GitHub source and include that as a module dependency by following these steps:
 * Clone this library into a project named SmartRecyclerView, parallel to your own application project:
```shell
git clone https://github.com/baldapps/SmartRecyclerView.git
```
 * In the root of your application's project edit the file "settings.gradle" and add the following lines:
```shell
include ':smartrecyclerview'
project(':smartrecyclerview').projectDir = new File('../Smartrecyclerview/smartrecyclerview/')
```
 * In your application's main module (usually called "app"), edit your build.gradle to add a new dependency:
```shell
 dependencies {
    ...
    compile project(':smartrecyclerview')
 }
```
Now your project is ready to use this library

## Main Features
Smart Recycler view adds to the support library version:
 * Add click and long listeners in an easy way using the recycler view
 * Add drag and swipe interface
 * Add the choice mode like a ListView, so you can use none, single, multi and multi modal
 Extend BaseViewHolder and RecyclerArrayAdapter in order to add your logic. Example and how to use the list:
 ```java
        public class MainActivity extends Activity implements DragListener, SmartRecycleView.OnItemClickListener, MultiChoiceModeListener {

           private ItemTouchHelper touchHelper;
           private TouchHelperCallback callback;

           @Override
           protected void onCreate(Bundle savedInstanceState) {
               super.onCreate(savedInstanceState);
               setContentView(R.layout.activity_main);

               SmartRecycleView list = findViewById(R.id.list);
               ArrayList<Item> items = new ArrayList<>();
               //add some stuff to items here
               ArrayAdapterTest adapter = new ArrayAdapterTest(this, items);
               adapter.setDragListener(this);

               list.setChoiceMode(CheckableList.SINGLE);
               list.setMultiChoiceModeListener(this);

               callback = new TouchHelperCallback(adapter);
               touchHelper = new ItemTouchHelper(callback);
               touchHelper.attachToRecyclerView(list);
               list.setLayoutManager(new LinearLayoutManager(this));
               list.setAdapter(adapter);
               list.addOnItemClickListener(this);
           }
        
           @Override
           public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
               touchHelper.startDrag(viewHolder);
           }

           @Override
           public void onItemClick(RecyclerView parent, View clickedView, int position) {
               Log.d("TAG", "item cliked at postion " + position);
           }

           @Override
           public void onItemLongClick(RecyclerView parent, View clickedView, int position) {
               Log.d("TAG", "item long cliked at postion " + position);
           }

           /* Action modes */
           @Override
           public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
           }

           @Override
           public boolean onCreateActionMode(ActionMode mode, Menu menu) {
              // Inflate a menu resource providing context menu items
              MenuInflater inflater = mode.getMenuInflater();
              inflater.inflate(R.menu.delete_command, menu);
              callback.enableDrag(false);
              return true;
           }

           @Override
           public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
              return false;
           }

           @Override
           public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
              switch (item.getItemId()) {
                case R.id.delete_device:
                   //do domething here
                   mode.finish(); // Action picked, so close the CAB
                   return true;
               default:
                  return false;
           }
    
           @Override
           public void onDestroyActionMode(ActionMode mode) {
              callback.enableDrag(true);
           }
 ```

## References and how to report bugs
* If you find any issues with this library, please open a bug here on GitHub

## License
See LICENSE

## Change List

1.0.0
 * First version
