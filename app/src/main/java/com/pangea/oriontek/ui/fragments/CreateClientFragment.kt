package com.pangea.oriontek.ui.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pangea.oriontek.R
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.result.contract.ActivityResultContracts
import com.pangea.oriontek.databinding.DialogImagePickerBinding
import com.pangea.oriontek.databinding.FragmentCreateClientBinding
import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.ui.fragments.states.CreateClientEvent
import com.pangea.oriontek.ui.fragments.states.CreateClientUiState
import com.pangea.oriontek.ui.fragments.states.ValidationErrors
import com.pangea.oriontek.ui.home.HomeActivity.Companion.ARG_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@AndroidEntryPoint
class CreateClientFragment : Fragment() {

    private lateinit var binding: FragmentCreateClientBinding
    private val viewModel: CreateClientViewModel by viewModels()


    private var cameraImageUri: Uri? = null


    // Lanzar la galleria
    /*private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        uri?.let {
            viewModel.updatePhoto(it.toString())
        }
    }*/

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        uri?.let {

            requireContext().contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            viewModel.updatePhoto(it.toString())
        }
    }


    // Lanzar la camara
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->

        if (success && cameraImageUri != null) {
            viewModel.updatePhoto(
                cameraImageUri.toString()
            )

        }

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateClientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getLong(ARG_ID, 0L) ?: 0L

        setupActionBar(id != 0L)
        setupMenu()
        setupListeners()
        setupTextWatchers()

        if (id != 0L) viewModel.loadClient(id)

        observeUiState()
        observeEvents()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CreateClientUiState.Form -> {
                            fillFields(state.data.client, state.data.addresses)
                            showErrors(state.errors)
                        }

                        is CreateClientUiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT)
                                .show()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is CreateClientEvent.Created -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.message_created_success,
                                Toast.LENGTH_SHORT
                            ).show()
                            clearFields()
                        }

                        is CreateClientEvent.Updated -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.message_updated_success,
                                Toast.LENGTH_SHORT
                            ).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }

                        is CreateClientEvent.ShowMessage -> {
                            Toast.makeText(requireContext(), event.resId, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun createImageUri(): Uri {
        val image = File.createTempFile(
            "camera_image_",
            ".jpg",
            requireContext().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES
            )
        )
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            image

        )
    }

    private fun setupTextWatchers() = with(binding) {
        etName.doAfterTextChanged { viewModel.updateClientField { copy(name = it.toString()) } }
        etLastname.doAfterTextChanged { viewModel.updateClientField { copy(lastName = it.toString()) } }
        etEmail.doAfterTextChanged { viewModel.updateClientField { copy(email = it.toString()) } }
        etCompany.doAfterTextChanged { viewModel.updateClientField { copy(company = it.toString()) } }
        etPhone.doAfterTextChanged { viewModel.updateClientField { copy(phone = it.toString()) } }
        etAddress.doAfterTextChanged { viewModel.updateAddress(0, it.toString()) }
        etAddress2.doAfterTextChanged { viewModel.updateAddress(1, it.toString()) }
    }

    // Llenar los campos
    private fun fillFields(client: Client, addresses: List<Address>) = with(binding) {
        setTextIfDifferent(etName, client.name)
        setTextIfDifferent(etLastname, client.lastName)
        setTextIfDifferent(etCompany, client.company)
        setTextIfDifferent(etEmail, client.email)
        setTextIfDifferent(etPhone, client.phone)

        setTextIfDifferent(etAddress, addresses.getOrNull(0)?.fullAddress.orEmpty())
        setTextIfDifferent(etAddress2, addresses.getOrNull(1)?.fullAddress.orEmpty())

        if (client.photoUri.isNotEmpty()) {

            try {

                imgPhoto.setImageURI(
                    client.photoUri.toUri()
                )

            } catch (_: SecurityException) {

                imgPhoto.setImageResource(
                    R.drawable.photo_01
                )

            } catch (_: Exception) {

                imgPhoto.setImageResource(
                    R.drawable.photo_01
                )
            }

        } else {

            imgPhoto.setImageResource(
                R.drawable.photo_01
            )
        }
    }

    private fun setTextIfDifferent(editText: EditText, newText: String) {
        if (editText.text.toString() != newText) {
            editText.setText(newText)
        }
    }

    private fun showErrors(e: ValidationErrors) = with(binding) {
        tilName.error = e.name
        tilLastname.error = e.lastName
        tilEmail.error = e.email
        tilCompany.error = e.company
        tilPhone.error = e.phone
        tilAddress.error = e.address
    }


    // Seleccionar foto (dialog)
    private fun selectPhoto() {

        val dialogBinding = DialogImagePickerBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_image)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialogBinding.btnCamera.setOnClickListener {
            cameraImageUri = createImageUri()
            cameraLauncher.launch(cameraImageUri)
            dialog.dismiss()
        }

        dialogBinding.btnGallery.setOnClickListener {
            galleryLauncher.launch(arrayOf("image/*"))
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun setupListeners() {
        binding.btnChangePhoto.setOnClickListener { selectPhoto() }
    }

    private fun setupActionBar(isEditMode: Boolean) {
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(if (isEditMode) R.string.update_client else R.string.create_client)
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.menu, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_save -> {
                        viewModel.saveClient(); true
                    }

                    android.R.id.home -> {
                        requireActivity().onBackPressedDispatcher.onBackPressed(); true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun clearFields() = with(binding) {
        listOf(
            etName,
            etLastname,
            etCompany,
            etEmail,
            etPhone,
            etAddress,
            etAddress2
        ).forEach { it.text?.clear() }
        listOf(
            tilName,
            tilLastname,
            tilEmail,
            tilCompany,
            tilPhone,
            tilAddress
        ).forEach { it.error = null }

        imgPhoto.setImageResource(
            R.drawable.photo_01
        )
        viewModel.updatePhoto("")
    }
}