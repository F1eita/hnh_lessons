package com.example.lesson_3_zhuravleva.presentation.ui.product

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.createViewModelLazy
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.lesson_3_zhuravleva.R
import com.example.lesson_3_zhuravleva.data.responsemodel.ResponseStates
import com.example.lesson_3_zhuravleva.databinding.FragmentProductBinding
import com.example.lesson_3_zhuravleva.domain.product.ProductInfo
import com.example.lesson_3_zhuravleva.domain.product.Size
import com.example.lesson_3_zhuravleva.presentation.ui.exception.getError
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class ProductFragment : Fragment(), ImagesAdapter.Listener {

    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!

    private val args: ProductFragmentArgs by navArgs()

    @Inject
    lateinit var imagesAdapter: ImagesAdapter

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by createViewModelLazy(
        ProductViewModel::class,
        { this.viewModelStore },
        factoryProducer = { viewModelFactory })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProductBinding.inflate(inflater, container, false)
        requireActivity().window.statusBarColor = resources.getColor(R.color.gray)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObserver()
        viewModel.getProduct(args.id)
        val imagesRecyclerView = binding.productScreen.rvImages
        imagesRecyclerView.adapter = imagesAdapter
        imagesRecyclerView.itemAnimator
        binding.errorScreen.btnUpdateData.setOnClickListener {
            viewModel.getProduct(args.id)
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addObserver(){
        viewModel.productLiveData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ResponseStates.Success -> {
                    val product = result.data
                    productScreenBind(product)
                    binding.viewFlipper.displayedChild = PRODUCT_SCREEN
                }

                is ResponseStates.Failure -> {
                    binding.errorScreen.tvErrorText.text = result.e.getError() ?: ""
                    binding.viewFlipper.displayedChild = ERROR_SCREEN
                }

                is ResponseStates.Loading -> {
                    binding.viewFlipper.displayedChild = LOADING_SCREEN
                }
            }
        }
    }

    private fun productScreenBind(product: ProductInfo){
        binding.apply {
            toolbar.title = product.title
            productScreen.apply {
                with(product){
                    tvPrice.text = getString(R.string.ruble, price.toString())
                    tvBadge.text = badge[0].value
                    tvBadge.backgroundTintList = ColorStateList.valueOf(Color
                        .parseColor(badge[0].color))
                    tvTitle.text = title
                    tvDepartment.text = department
                    tvDescription.text = description

                    val imagesList = mutableListOf<ImageItem>()
                    for (i in 0..2){
                        var image: String? = null
                        when(i){
                            0 -> image = preview
                            in 1..images.size -> image = images[i-1]
                        }
                        imagesList.add(ImageItem(i, image, false))
                    }
                    imagesAdapter.submitList(imagesList)
                    imagesAdapter.selectedItem = imagesList[0]

                    for (i in details){
                        tvDetails.append(resources.getString(R.string.marker, i))
                    }
                    insertImage(preview)

                    textBoxSize.setEndIconOnClickListener {
                        toSizesSheet(sizes)
                    }

                    tvSize.setOnClickListener {
                        toSizesSheet(sizes)
                    }

                    setFragmentResultListener(REQUEST_KEY){ _, bundle ->
                        val size: Size? = bundle.getParcelable(BUNDLE_KEY)
                        tvSize.setText(size?.value ?: "")
                    }
                }
            }
        }
    }

    private fun insertImage(image: String?){
        binding.productScreen.apply {
            Glide.with(productImage)
                .load(image ?: R.drawable.base_image )
                .transform(
                    MultiTransformation(
                        CenterCrop(),
                        RoundedCorners(resources
                            .getDimension(R.dimen.corner_radius_100).toInt())
                    )
                ).into(productImage)
        }
    }

    private fun toSizesSheet(sizes: List<Size>){
        val sizesTypedArray = sizes.toTypedArray()
        val action = ProductFragmentDirections
            .actionProductFragmentToSizesBottomSheetFragment(sizelist = sizesTypedArray)
        findNavController().navigate(action)
    }

    companion object{
        const val PRODUCT_SCREEN = 0
        const val ERROR_SCREEN = 1
        const val LOADING_SCREEN = 2

        const val REQUEST_KEY = "requestKey"
        const val BUNDLE_KEY = "bundle"
    }

    override fun onClick(image: ImageItem) {
        imagesAdapter.selectedItem = image
        insertImage(image.image)
    }
}